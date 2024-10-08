package com.katzheimer.testfolder

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GameLowActivity : BaseActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var quizList: List<SingletonKotlin.QuizItem>
    private lateinit var selectedQuizzes: List<SingletonKotlin.QuizItem>
    private var currentRound = 0
    private var correctAnswers = 0
    private var startTime: Long = 0
    private var totalTime = 0L      //현재 토탈타임은 맞춘 문제에 대한 시간만 저장하는 것으로 변경되었음 주의.
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

    private lateinit var obutton: FrameLayout
    private lateinit var xbutton: FrameLayout

    private lateinit var getCoin: MediaPlayer   //효과음 재생용 변수
    private lateinit var wrong: MediaPlayer   //효과음 재생용 변수

    private lateinit var correctWrongOverlay: FrameLayout
    private lateinit var correctWrongImage: ImageView

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_low)

        // Initialize if it's not initialized
        if (!SingletonKotlin.isInitialized()) {
            SingletonKotlin.initialize(auth, database)
        }

        sharedPreferences = getSharedPreferences("game_settings", Context.MODE_PRIVATE)

        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox) // 질문텍스트
        obutton = findViewById(R.id.o_btn)  // O버튼
        xbutton = findViewById(R.id.x_btn) // X버튼
        coinText = findViewById(R.id.coin_text)

        roundImageView = findViewById(R.id.roundImageView)
        numberImageView = findViewById(R.id.numberImageView)
        finishedTextView = findViewById(R.id.finishedTextView)

        loadingLayout = findViewById(R.id.loading_layout)
        loadingImage = findViewById(R.id.loading_image)
        loadingText = findViewById(R.id.loading_text)

        correctWrongOverlay = findViewById(R.id.correct_wrong_overlay)
        correctWrongImage = findViewById(R.id.correct_wrong_image)

        getCoin = MediaPlayer.create(this, R.raw.coin)
        wrong = MediaPlayer.create(this, R.raw.wrong)
        // 로딩 이미지 회전 애니메이션 적용
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loadingImage.startAnimation(rotateAnimation)
        startLoadingTextAnimation()

        // 답변 버튼 비활성화
        obutton.isEnabled = false
        xbutton.isEnabled = false

        try {
            currentUser = SingletonKotlin.getCurrentUser() ?: throw IllegalStateException("User authentication required.")
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin is not initialized.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 사용자 코인 불러오기
        try {
            SingletonKotlin.loadUserCoins(coinText)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin is not initialized.", Toast.LENGTH_SHORT).show()
            finish()
        }

        val excludeIds = setOf(R.id.o_btn, R.id.x_btn)
        applyFontSize(excludeIds)

        // 3초 동안 로딩 화면 표시 후 게임 시작
        handler.postDelayed({
            loadingLayout.visibility = View.GONE
            // 설정된 기간에 맞게 퀴즈 데이터 필터링
            SingletonKotlin.loadOXQuizData { quizData ->
                quizList = filterQuizDataByPeriod(quizData)
                if (quizList.isNotEmpty()) {
                    selectedQuizzes = quizList.shuffled().take(5)
                    startRound(questionTextView)
                    // 답변 버튼 활성화
                    obutton.isEnabled = true
                    xbutton.isEnabled = true
                } else {
                    showNoQuizzesDialogAndExit()
                }
            }
        }, 3000)

        obutton.setOnClickListener {
            handleAnswer("O", questionTextView)
        }

        xbutton.setOnClickListener {
            handleAnswer("X", questionTextView)
        }
    }

    private fun filterQuizDataByPeriod(quizData: List<SingletonKotlin.QuizItem>): List<SingletonKotlin.QuizItem> {
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


    private fun startRound(questionTextView: TextView) {
        startTime = System.currentTimeMillis()
        progressStatus = 0
        progressBar.progress = progressStatus
        updateRoundImages()
        displayQuestion(questionTextView)
        startProgressBar(questionTextView)
    }

    private fun displayQuestion(questionTextView: TextView) {
        if (currentRound < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentRound]
            questionTextView.text = "Date: ${quizItem.date}\n\n${quizItem.question}" //4지선다랑 형식 통일화
        } else {
            val totalGameTime = totalTime / 1000 // 초 단위로 변환
            SingletonKotlin.saveGameResult("OX", correctAnswers, totalGameTime) // 게임 유형 추가
            showGameResultDialog(correctAnswers, totalGameTime, incorrectQuestions)
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView) {
        if (currentRound < selectedQuizzes.size) {
            // 정답을 처리하는 동안 버튼 비활성화
            obutton.isEnabled = false
            xbutton.isEnabled = false

            val quizItem = selectedQuizzes[currentRound]
            if (userAnswer == quizItem.answer) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                correctAnswers++
                totalTime += System.currentTimeMillis() - startTime
                SingletonKotlin.updateUserCoins(5, coinText)
                getCoin.start()
                showCorrectWrongImage(R.drawable.correct_cat)
            } else {
                wrong.start()
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
                incorrectQuestions.add(Pair(currentRound + 1, quizItem.date)) // 틀린 문제 번호와 날짜 추가
                showCorrectWrongImage(R.drawable.wrong_cat)
            }
        }
    }


    private fun showCorrectWrongImage(imageResId: Int) {
        correctWrongImage.setImageResource(imageResId)
        correctWrongOverlay.visibility = View.VISIBLE

        handler.postDelayed({
            correctWrongOverlay.visibility = View.GONE
            currentRound++
            // 다음 문제로 넘어갈 때 버튼을 활성화
            obutton.isEnabled = true
            xbutton.isEnabled = true
            startRound(findViewById(R.id.qeustionbox))
        }, 2000)
    }

    private fun startProgressBar(questionTextView: TextView) {
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
                if (currentRound < selectedQuizzes.size) {
                    questionTextView.text = "Time's up!"
                    currentRound++
                    startRound(questionTextView)
                }
            }
        }.apply { start() }
    }

    private fun updateRoundImages() {
        val roundDrawable = R.drawable.round // round.png 이미지 리소스
        val numberDrawable = when (currentRound + 1) {
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

    override fun onDestroy() {
        super.onDestroy()
        progressBarThread?.interrupt()
    }
}
