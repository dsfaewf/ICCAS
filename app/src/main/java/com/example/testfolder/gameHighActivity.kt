package com.example.testfolder

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseUser

class gameHighActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    //퀴즈 처리용 변수들
    private lateinit var quizList: List<SingletonKotlin.BlankQuizItem>
    private var currentQuestionIndex = 0
    private var correctAnswers = 0
    private var startTime = 0L
    private val totalRounds = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_high)
        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox)
        val answerEditText = findViewById<EditText>(R.id.answer_edit_text) //id가 리소스에 없길래 추가해서 새로 만듬
        val submitBtn = findViewById<Button>(R.id.submit_btn)
        coinText = findViewById(R.id.coin_text)

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

        SingletonKotlin.loadBlankQuizData { quizData ->
            if (quizData.isNotEmpty()) {
                quizList = quizData.shuffled().take(totalRounds)
                startTime = System.currentTimeMillis()
                displayQuestion(questionTextView, answerEditText)
            } else {
                questionTextView.text = "No quiz available."
            }
        }

        submitBtn.setOnClickListener {
            handleAnswer(answerEditText.text.toString(), questionTextView, answerEditText)
        }

        startProgressBar(questionTextView)
    }

    private fun displayQuestion(questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            questionTextView.text = "Date: ${quizItem.date}\n\n${quizItem.question}"
            answerEditText.text.clear()
        }
    }

    // 4지선다에서 만들어둔거 가져다 썼음
    private fun handleAnswer(userAnswer: String, questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            // 정답 판별을 위해 Log로 확인
            Log.d("Quiz", "User Answer: $userAnswer, Correct Answer: ${quizItem.answer}")
            if (userAnswer.trim().equals(quizItem.answer.trim(), ignoreCase = true)) {
                correctAnswers++
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                SingletonKotlin.updateUserCoins(5, coinText)
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
            }
            currentQuestionIndex++
            if (currentQuestionIndex < quizList.size) {
                displayQuestion(questionTextView, answerEditText)
            } else {
                val totalTime = System.currentTimeMillis() - startTime
                SingletonKotlin.saveGameResult("Short Answer", correctAnswers, totalTime)
                questionTextView.text = "Quiz completed! Correct answers: $correctAnswers, Total time: ${totalTime / 1000} seconds\nReturning to game selection screen in 5 seconds..."
                handler.postDelayed({
                    finish()
                }, 5000)
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
            handler.post { // 60초 넘었을 때,
                questionTextView.text = "Time's up!"
            }
        }.start()
    }
}
