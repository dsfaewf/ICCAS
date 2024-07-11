package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import kotlin.random.Random

class gameLowActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_low)
        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox) // 질문텍스트
        val obutton = findViewById<Button>(R.id.o_btn)  // O버튼
        val xbutton = findViewById<Button>(R.id.x_btn) // X버튼
        coinText = findViewById(R.id.coin_text)

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

        // 랜덤으로 5개의 OX 퀴즈 데이터 불러오기
        SingletonKotlin.loadOXQuizData { quizData ->
            quizList = quizData
            if (quizList.size >= 5) {
                selectedQuizzes = quizList.shuffled().take(5)
                startRound(questionTextView)
            } else {
                questionTextView.text = "Not enough quizzes available."
            }
        }

        obutton.setOnClickListener {
            handleAnswer("O", questionTextView)
        }

        xbutton.setOnClickListener {
            handleAnswer("X", questionTextView)
        }
        startTime = System.currentTimeMillis()
        startProgressBar(questionTextView)
    }

    private fun startRound(questionTextView: TextView) {
        currentRound = 0
        correctAnswers = 0
        displayQuestion(questionTextView)
    }

    private fun displayQuestion(questionTextView: TextView) {
        if (currentRound < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentRound]
            questionTextView.text = "Date: ${quizItem.date}\n\n${quizItem.question}" //4지선다랑 형식 통일화
        } else {
            val totalTime = System.currentTimeMillis() - startTime
            SingletonKotlin.saveGameResult("OX", correctAnswers, totalTime) // 게임 유형 추가
            questionTextView.text = "Quiz completed! Correct answers: $correctAnswers, Time taken: ${totalTime / 1000} seconds\nReturning to game selection screen in 5 seconds..."
            handler.postDelayed({
                finish() //그냥 이전 화면으로 돌아가기
            }, 5000) // 나가기 전에 5 seconds 딜레이
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView) {
        if (currentRound < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentRound]
            if (userAnswer == quizItem.answer) {
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                correctAnswers++
                SingletonKotlin.updateUserCoins(5, coinText)
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
            }
            currentRound++
            if (currentRound < selectedQuizzes.size) {
                displayQuestion(questionTextView)
            } else {
                displayQuestion(questionTextView)
            }
        }
    }

    private fun startProgressBar(questionTextView: TextView) {
        Thread {
            while (progressStatus < 300) {  // 0.2 * 300 = 60초
                progressStatus += 1
                handler.post {
                    progressBar.progress = progressStatus
                }
                try {
                    Thread.sleep(200)   // 0.2초 대기
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            handler.post {  // 60초 넘었을 때,
                questionTextView.text = "Time's up!"
            }
        }.start()
    }
}
