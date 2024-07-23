package com.katzheimer.testfolder

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.katzheimer.testfolder.utils.PreprocessTexts
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GameHighActivity : BaseActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var quizList: List<SingletonKotlin.BlankQuizItem>
    private lateinit var selectedQuizzes: List<SingletonKotlin.BlankQuizItem>
    private lateinit var date: String
    private lateinit var answer: String
    private var isTextChangedListenerActive: Boolean = true
    private var indexOfFirstUnderbar = 0
    private var currentQuestionIndex = 0
    private var correctAnswers = 0
    private var startTime = 0L
    private var totalTime = 0L
    private val totalRounds = 5
    private val roundTime = 60 * 1000 // 60초
    private var progressBarThread: Thread? = null
    private val incorrectQuestions = mutableListOf<Pair<Int, String>>() // 틀린 문제 번호와 날짜 저장
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화

    private lateinit var roundImageView: ImageView
    private lateinit var numberImageView: ImageView
    private lateinit var finishedTextView: TextView

    private lateinit var loadingLayout: View
    private lateinit var loadingImage: ImageView
    private lateinit var loadingText: TextView

    private lateinit var answerEditText: EditText
    private lateinit var submitBtn: Button
    private lateinit var hintButton: Button

    private lateinit var getCoin: MediaPlayer
    private lateinit var wrong: MediaPlayer

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var correctWrongOverlay: FrameLayout
    private lateinit var correctWrongImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_high)

        // Initialize if it's not initialized
        if (!SingletonKotlin.isInitialized()) {
            SingletonKotlin.initialize(auth, database)
        }

        sharedPreferences = getSharedPreferences("game_settings", Context.MODE_PRIVATE)

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

        getCoin = MediaPlayer.create(this, R.raw.coin)
        wrong = MediaPlayer.create(this, R.raw.wrong)

        correctWrongOverlay = findViewById(R.id.correct_wrong_overlay)
        correctWrongImage = findViewById(R.id.correct_wrong_image)

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
                quizList = filterQuizDataByPeriod(quizData)
                if (quizList.isNotEmpty()) {
                    selectedQuizzes = quizList.shuffled().take(totalRounds) // 랜덤으로 문제를 섞고 최대 totalRounds개 선택
                    startTime = System.currentTimeMillis()
                    displayQuestion(questionTextView, answerEditText)
                    startProgressBar(questionTextView, answerEditText)
                    // 답변 버튼 활성화
                    enableAnswerControls()
                } else {
                    showNoQuizzesDialogAndExit()
                }
            }
        }, 3000)

        submitBtn.setOnClickListener {
            isTextChangedListenerActive = false
            var userAnswer = answerEditText.text.toString()
            if (userAnswer.length > answer.length) {
                userAnswer = userAnswer.substring(0, answer.length)
            }
            handleAnswer(userAnswer, questionTextView, answerEditText)
        }

        hintButton.setOnClickListener {
            showHintDialog()
        }

        answerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if (isTextChangedListenerActive) {
                        val questionText = questionTextView.text.toString()
                        var underbarIndex = indexOfFirstUnderbar
                        val sb = StringBuilder(questionText)
                        for (i in 0 until answer.length) {
                            if (i < s.length) {
                                sb.setCharAt(underbarIndex, s[i])
                            } else {
                                sb.setCharAt(underbarIndex, '_')
                            }
                            underbarIndex += 1
                        }
                        questionTextView.text = sb.toString()
                    }
                }
            }
        })
    }
    private fun filterQuizDataByPeriod(quizData: List<SingletonKotlin.BlankQuizItem>): List<SingletonKotlin.BlankQuizItem> {
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
        val confirmDialog = ConfirmDialog(
            object : DialogCustomInterface {
                override fun onClickYesButton(id: Int) {
                    finish() // Close the activity when the "OK" button is clicked
                }
            },
            title = "No Quizzes Available",
            content = "There are no quiz entries available for the selected period. Please try a different period or add more quiz entries.",
            buttonText = "OK",
            id = -1
        )

        confirmDialog.isCancelable = false
        confirmDialog.show(supportFragmentManager, "ConfirmDialog")
    }

    private fun showCorrectWrongImage(imageResId: Int) {
        correctWrongImage.setImageResource(imageResId)
        correctWrongOverlay.visibility = View.VISIBLE
        handler.postDelayed({
            correctWrongOverlay.visibility = View.GONE
            currentQuestionIndex++
            if (currentQuestionIndex < selectedQuizzes.size) {
                startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                displayQuestion(findViewById(R.id.qeustionbox), findViewById(R.id.answer_edit_text))
                startProgressBar(findViewById(R.id.qeustionbox), findViewById(R.id.answer_edit_text))
                // 다음 문제로 넘어갈 때 버튼 활성화
                enableAnswerControls()
            } else {
                endQuiz()
            }
        }, 2000) // 2초 동안 이미지를 표시
    }


    private fun displayQuestion(questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentQuestionIndex]
            val answerLength = quizItem.answer.length
            val question = quizItem.question.replace("<blank>", "_".repeat(answerLength))
            questionTextView.text = "Date: ${quizItem.date}\n\n${question}"
            answerEditText.text.clear()
            this.date = quizItem.date
            this.answer = quizItem.answer
            this.indexOfFirstUnderbar = questionTextView.text.indexOf('_')
            updateRoundImages()
            isTextChangedListenerActive = true
        } else {
            endQuiz()
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            // 정답을 처리하는 동안 버튼 비활성화
            disableAnswerControls()

            val quizItem = selectedQuizzes[currentQuestionIndex]
            val inputStr = userAnswer.trim()
            val answerStr = quizItem.answer.trim()
            Log.d("Quiz", "User Answer: $userAnswer, Correct Answer: ${quizItem.answer}")
            if (PreprocessTexts.isCorrectAnswer(inputStr, answerStr)) {
                correctAnswers++
                showCorrectWrongImage(R.drawable.correct_cat)
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                SingletonKotlin.updateUserCoins(5, coinText)
                getCoin.start()
                totalTime += System.currentTimeMillis() - startTime
            } else {
                showCorrectWrongImage(R.drawable.wrong_cat)
                wrong.start()
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
                incorrectQuestions.add(Pair(currentQuestionIndex + 1, quizItem.date)) // 틀린 문제 번호와 날짜 추가
            }
//        currentQuestionIndex++
            if (currentQuestionIndex < quizList.size) {
                startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                displayQuestion(questionTextView, answerEditText)
                startProgressBar(questionTextView, answerEditText)
                // 다음 문제로 넘어갈 때 버튼 활성화
                enableAnswerControls()
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
            while (progressStatus < 300 && (System.currentTimeMillis() - startRoundTime) < roundTime) {
                progressStatus += 1
                handler.post {
                    progressBar.progress = progressStatus
                }
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    return@Thread
                }
            }
            handler.post {
                if (currentQuestionIndex < selectedQuizzes.size) {  // selectedQuizzes로 변경
                    questionTextView.text = "Time's up!"
                    currentQuestionIndex++
                    if (currentQuestionIndex < selectedQuizzes.size) {  // selectedQuizzes로 변경
                        startTime = System.currentTimeMillis()
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

        val confirmDialog = ConfirmDialog(
            object : DialogCustomInterface {
                override fun onClickYesButton(id: Int) {
                    finish() // Close the activity when the "OK" button is clicked
                }
            },
            title = "Game Result",
            content = " Quiz completed!\n\n*Correct answers: $correctAnswers\nTime taken for correct answers: $totalTimeSeconds sec\n\n* Incorrect questions\n$incorrectQuestionsText",
            buttonText = "OK",
            id = -1,
        )

        confirmDialog.isCancelable = false
        confirmDialog.show(supportFragmentManager, "ConfirmDialog")
    }

    private fun showHintDialog() {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = selectedQuizzes[currentQuestionIndex]
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
