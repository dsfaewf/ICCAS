package com.example.testfolder

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.testfolder.utils.PreprocessTexts
import com.google.firebase.auth.FirebaseUser

class gameHighActivity : BaseActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var quizList: List<SingletonKotlin.BlankQuizItem>
    private var currentQuestionIndex = 0
    private var correctAnswers = 0
    private var startTime = 0L
    private var totalTime = 0L
    private val totalRounds = 5
    private val roundTime = 60 * 1000 // 60초
    private var progressBarThread: Thread? = null
    private val incorrectQuestions = mutableListOf<Pair<Int, String>>() // 틀린 문제 번호와 날짜 저장

    private lateinit var roundImageView: ImageView
    private lateinit var numberImageView: ImageView
    private lateinit var finishedTextView: TextView

    private lateinit var loadingLayout: View
    private lateinit var loadingImage: ImageView
    private lateinit var loadingText: TextView

    private lateinit var answerEditText: EditText
    private lateinit var submitBtn: Button
    private lateinit var hintButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_high)
        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox)
        answerEditText = findViewById(R.id.answer_edit_text)
        submitBtn = findViewById(R.id.submit_btn)
        coinText = findViewById(R.id.coin_text)
        hintButton = findViewById(R.id.hint_button)

        roundImageView = findViewById(R.id.roundImageView)
        numberImageView = findViewById(R.id.numberImageView)
        finishedTextView = findViewById(R.id.finishedTextView)

        loadingLayout = findViewById(R.id.loading_layout)
        loadingImage = findViewById(R.id.loading_image)
        loadingText = findViewById(R.id.loading_text)

        // 로딩 이미지 회전 애니메이션 적용
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loadingImage.startAnimation(rotateAnimation)

        // 로딩 텍스트 애니메이션 적용
        startLoadingTextAnimation()

        // 답변 버튼 비활성화
        disableAnswerControls()

        try {
            currentUser = SingletonKotlin.getCurrentUser() ?: throw IllegalStateException("User authentication required.")
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin is not initialized.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            SingletonKotlin.loadUserCoins(coinText)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin is not initialized.", Toast.LENGTH_SHORT).show()
            finish()
        }
        applyFontSize()

        // 3초 동안 로딩 화면 표시 후 게임 시작
        handler.postDelayed({
            loadingLayout.visibility = View.GONE
            SingletonKotlin.loadBlankQuizData { quizData ->
                if (quizData.isNotEmpty()) {
                    quizList = quizData.shuffled().take(totalRounds)
                    startTime = System.currentTimeMillis()
                    displayQuestion(questionTextView, answerEditText)
                    startProgressBar(questionTextView, answerEditText)
                    // 답변 버튼 활성화
                    enableAnswerControls()
                } else {
//                    questionTextView.text = "No quiz available."
                    SingletonKotlin.showNoQuizzesDialogAndExit(this)
                }
            }
        }, 3000)

        submitBtn.setOnClickListener {
            handleAnswer(answerEditText.text.toString(), questionTextView, answerEditText)
        }

        hintButton.setOnClickListener {
            showHintDialog()
        }
    }

    private fun displayQuestion(questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            val answerLength = quizItem.answer.length
            val question = quizItem.question.replace("<blank>", "_".repeat(answerLength))

            questionTextView.text = "Date: ${quizItem.date}\n\n${question}"
            answerEditText.text.clear()
            updateRoundImages()
        } else {
            endQuiz()
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            val inputStr = userAnswer.trim()
            val answerStr = quizItem.answer.trim()
            Log.d("Quiz", "User Answer: $userAnswer, Correct Answer: ${quizItem.answer}")
//            if (userAnswer.trim().equals(quizItem.answer.trim(), ignoreCase = true)) {
            if (PreprocessTexts.isCorrectAnswer(inputStr, answerStr)) {
                correctAnswers++
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                SingletonKotlin.updateUserCoins(5, coinText)
                totalTime += System.currentTimeMillis() - startTime
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
                incorrectQuestions.add(Pair(currentQuestionIndex + 1, quizItem.date)) // 틀린 문제 번호와 날짜 추가
            }
            currentQuestionIndex++
            if (currentQuestionIndex < quizList.size) {
                startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                displayQuestion(questionTextView, answerEditText)
                startProgressBar(questionTextView, answerEditText)
            } else {
                endQuiz()
            }
        }
    }

    private fun endQuiz() {
        val totalTimeSeconds = totalTime / 1000 // 초 단위로 변환
        SingletonKotlin.saveGameResult("Short Answer", correctAnswers, totalTimeSeconds)
        showGameResultDialog(correctAnswers, totalTimeSeconds, incorrectQuestions)
    }

    private fun startProgressBar(questionTextView: TextView, answerEditText: EditText) {
        progressStatus = 0
        progressBar.progress = progressStatus
        progressBarThread?.interrupt()
        progressBarThread = Thread {
            val startRoundTime = System.currentTimeMillis()
            while (progressStatus < 300 && (System.currentTimeMillis() - startRoundTime) < roundTime) {  // 0.2 * 300 = 60초
                progressStatus += 1
                handler.post {
                    progressBar.progress = progressStatus
                }
                try {
                    Thread.sleep(200)   // 0.2초 대기
                } catch (e: InterruptedException) {
                    return@Thread
                }
            }
            handler.post {
                if (currentQuestionIndex < quizList.size) {
                    questionTextView.text = "Time's up!"
                    currentQuestionIndex++
                    if (currentQuestionIndex < quizList.size) {
                        startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                        displayQuestion(questionTextView, answerEditText)
                        startProgressBar(questionTextView, answerEditText)
                    } else {
                        endQuiz()
                    }
                }
            }
        }.apply { start() }
    }

    private fun updateRoundImages() {
        val roundDrawable = R.drawable.round // round.png 이미지 리소스
        val numberDrawable = when (currentQuestionIndex + 1) {
            1 -> R.drawable.number1
            2 -> R.drawable.number2
            3 -> R.drawable.number3
            4 -> R.drawable.number4
            5 -> R.drawable.number5
            else -> null
        }
        roundImageView.setImageResource(roundDrawable)
        if (numberDrawable != null) {
            numberImageView.visibility = View.VISIBLE
            finishedTextView.visibility = View.GONE
            numberImageView.setImageResource(numberDrawable)
        } else {
            numberImageView.visibility = View.GONE
            finishedTextView.visibility = View.VISIBLE
        }
    }

    private fun startLoadingTextAnimation() { //...을 로딩과 함께 애니메이션으로 움직이도록
        var dotCount = 0
        handler.post(object : Runnable {
            override fun run() {
                dotCount++
                if (dotCount > 3) {
                    dotCount = 0
                }
                val dots = ".".repeat(dotCount)
                loadingText.text = "Pulling out the diary$dots"
                handler.postDelayed(this, 500) // 500ms마다 업데이트
            }
        })
    }

    private fun disableAnswerControls() {
        submitBtn.isEnabled = false
        answerEditText.isEnabled = false
    }

    private fun enableAnswerControls() {
        submitBtn.isEnabled = true
        answerEditText.isEnabled = true
    }

    private fun showGameResultDialog(correctAnswers: Int, totalTimeSeconds: Long, incorrectQuestions: List<Pair<Int, String>>) {
        val incorrectQuestionsText = if (incorrectQuestions.isNotEmpty()) {
            incorrectQuestions.joinToString("\n") { "Question ${it.first}: ${it.second}" }
        } else {
            "None"
        }

        val message = """
            Quiz completed!
            Correct answers: $correctAnswers
            Time taken for correct answers: $totalTimeSeconds seconds
            --------------------
            Incorrect questions:
            $incorrectQuestionsText
            """.trimIndent()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Result")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish() // 이전 화면으로 돌아가기
            }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showHintDialog() {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            val hintMessage = quizItem.hint

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hint")
                .setMessage(hintMessage)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressBarThread?.interrupt()
    }
}
