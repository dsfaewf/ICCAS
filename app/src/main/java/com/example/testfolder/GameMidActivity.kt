package com.example.testfolder

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GameMidActivity : BaseActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var quizList: List<SingletonKotlin.MultipleChoiceQuizItem>
    private lateinit var selectedQuizzes: List<SingletonKotlin.MultipleChoiceQuizItem>
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

    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button

    private lateinit var getCoin: MediaPlayer
    private lateinit var wrong: MediaPlayer

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_mid)

        sharedPreferences = getSharedPreferences("game_settings", Context.MODE_PRIVATE)

        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        btn4 = findViewById(R.id.btn4)
        coinText = findViewById(R.id.coin_text)

        roundImageView = findViewById(R.id.roundImageView)
        numberImageView = findViewById(R.id.numberImageView)
        finishedTextView = findViewById(R.id.finishedTextView)

        loadingLayout = findViewById(R.id.loading_layout)
        loadingImage = findViewById(R.id.loading_image)
        loadingText = findViewById(R.id.loading_text)
        getCoin = MediaPlayer.create(this, R.raw.coin)
        wrong = MediaPlayer.create(this, R.raw.wrong)
        // 로딩 이미지 회전 애니메이션 적용
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loadingImage.startAnimation(rotateAnimation)

        // 로딩 텍스트 애니메이션 적용
        startLoadingTextAnimation()

        // 답변 버튼 비활성화
        disableAnswerButtons()

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
            // 설정된 기간에 맞게 퀴즈 데이터 필터링
            SingletonKotlin.loadMultipleChoiceQuizData { quizData ->
                quizList = filterQuizDataByPeriod(quizData)
                if (quizList.size >= 5) {
                    selectedQuizzes = quizList.shuffled().take(totalRounds) // 랜덤으로 문제를 섞고 5개만 선택
                    startTime = System.currentTimeMillis()
                    displayQuestion(questionTextView, btn1, btn2, btn3, btn4)
                    startProgressBar(questionTextView, btn1, btn2, btn3, btn4)
                    // 답변 버튼 활성화
                    enableAnswerButtons()
                } else {
                    showNoQuizzesDialogAndExit()
                }
            }
        }, 3000)

        btn1.setOnClickListener { handleAnswer(btn1.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
        btn2.setOnClickListener { handleAnswer(btn2.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
        btn3.setOnClickListener { handleAnswer(btn3.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
        btn4.setOnClickListener { handleAnswer(btn4.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
    }

    private fun filterQuizDataByPeriod(quizData: List<SingletonKotlin.MultipleChoiceQuizItem>): List<SingletonKotlin.MultipleChoiceQuizItem> {
        val selectedPeriod = sharedPreferences.getString("selected_period", "Random")
        if (selectedPeriod == "Random") return quizData //기존이랑 같음
        val currentTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
        return quizData.filter {
            val quizTime = dateFormat.parse(it.date).time
            when (selectedPeriod) {
                "Within 3 days" -> TimeUnit.MILLISECONDS.toDays(currentTime - quizTime) <= 3
                "3 days to 1 week" -> TimeUnit.MILLISECONDS.toDays(currentTime - quizTime) in 4..7
                "1 week to 2 weeks" -> TimeUnit.MILLISECONDS.toDays(currentTime - quizTime) in 8..14
                "2 weeks to 1 month" -> TimeUnit.MILLISECONDS.toDays(currentTime - quizTime) in 15..30
                "1 month to 6 months" -> TimeUnit.MILLISECONDS.toDays(currentTime - quizTime) in 31..180
                else -> true
            }
        }
    }

    private fun showNoQuizzesDialogAndExit() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Quizzes Available")
            .setMessage("There are no diary entries available for the selected period.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun displayQuestion(questionTextView: TextView, btn1: Button, btn2: Button, btn3: Button, btn4: Button) {
        if (currentQuestionIndex < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentQuestionIndex]
            questionTextView.text = "Date: ${quizItem.date}\n\n${quizItem.question}"
            if (quizItem.choices.size >= 4) {
                btn1.text = quizItem.choices[0]
                btn2.text = quizItem.choices[1]
                btn3.text = quizItem.choices[2]
                btn4.text = quizItem.choices[3]
            } else {
                questionTextView.text = "Error: Not enough options available for this question."
            }
            updateRoundImages()
        } else {
            endQuiz(questionTextView)
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView, btn1: Button, btn2: Button, btn3: Button, btn4: Button) {
        if (currentQuestionIndex < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentQuestionIndex]
            Log.d("Quiz", "User Answer: $userAnswer, Correct Answer: ${quizItem.answer}")
            if (userAnswer == quizItem.answer) {
                correctAnswers++
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                SingletonKotlin.updateUserCoins(5, coinText) // Correct answer rewards 5 coins
                getCoin.start()
                totalTime += System.currentTimeMillis() - startTime
            } else {
                wrong.start()
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
                incorrectQuestions.add(Pair(currentQuestionIndex + 1, quizItem.date)) // 틀린 문제 번호와 날짜 추가
            }
            currentQuestionIndex++
            if (currentQuestionIndex < selectedQuizzes.size) {
                startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                displayQuestion(questionTextView, btn1, btn2, btn3, btn4)
                startProgressBar(questionTextView, btn1, btn2, btn3, btn4)
            } else {
                endQuiz(questionTextView)
            }
        }
    }

    private fun endQuiz(questionTextView: TextView) {
        val totalTimeSeconds = totalTime / 1000 // 초 단위로 변환
        SingletonKotlin.saveGameResult("4 Choice", correctAnswers, totalTimeSeconds)
        showGameResultDialog(correctAnswers, totalTimeSeconds, incorrectQuestions)
    }

    private fun startProgressBar(questionTextView: TextView, btn1: Button, btn2: Button, btn3: Button, btn4: Button) {
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
                if (currentQuestionIndex < selectedQuizzes.size) {
                    questionTextView.text = "Time's up!"
                    currentQuestionIndex++
                    if (currentQuestionIndex < selectedQuizzes.size) {
                        startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                        displayQuestion(questionTextView, btn1, btn2, btn3, btn4)
                        startProgressBar(questionTextView, btn1, btn2, btn3, btn4)
                    } else {
                        endQuiz(questionTextView)
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

    private fun disableAnswerButtons() {
        btn1.isEnabled = false
        btn2.isEnabled = false
        btn3.isEnabled = false
        btn4.isEnabled = false
    }

    private fun enableAnswerButtons() {
        btn1.isEnabled = true
        btn2.isEnabled = true
        btn3.isEnabled = true
        btn4.isEnabled = true
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

    override fun onDestroy() {
        super.onDestroy()
        progressBarThread?.interrupt()
    }
}
