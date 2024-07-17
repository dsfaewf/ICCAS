package com.example.testfolder

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
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseUser
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

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_low)

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
                if (quizList.size >= 5) {
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
            val quizItem = selectedQuizzes[currentRound]
            if (userAnswer == quizItem.answer) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                correctAnswers++
                totalTime += System.currentTimeMillis() - startTime
                SingletonKotlin.updateUserCoins(5, coinText)
                getCoin.start()
            } else {
                wrong.start()
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
                incorrectQuestions.add(Pair(currentRound + 1, quizItem.date)) // 틀린 문제 번호와 날짜 추가
            }
            currentRound++
            startRound(questionTextView)
        }
    }

    private fun startProgressBar(questionTextView: TextView) {
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
