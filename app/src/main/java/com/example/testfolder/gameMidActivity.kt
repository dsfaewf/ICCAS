package com.example.testfolder

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseUser

class gameMidActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var quizList: List<SingletonKotlin.MultipleChoiceQuizItem>
    private var currentQuestionIndex = 0
    private var correctAnswers = 0
    private var startTime = 0L
    private val totalRounds = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_mid)
        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox)
        val btn1 = findViewById<Button>(R.id.btn1)
        val btn2 = findViewById<Button>(R.id.btn2)
        val btn3 = findViewById<Button>(R.id.btn3)
        val btn4 = findViewById<Button>(R.id.btn4)
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

        // 퀴즈를 불러옴
        SingletonKotlin.loadMultipleChoiceQuizData { quizData ->
            if (quizData.isNotEmpty()) {
                quizList = quizData.shuffled().take(totalRounds) // 랜덤으로 문제를 섞고 5개만 선택
                startTime = System.currentTimeMillis()
                displayQuestion(questionTextView, btn1, btn2, btn3, btn4)
            } else {
                questionTextView.text = "No quiz available."
            }
        }

        btn1.setOnClickListener { handleAnswer(btn1.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
        btn2.setOnClickListener { handleAnswer(btn2.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
        btn3.setOnClickListener { handleAnswer(btn3.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }
        btn4.setOnClickListener { handleAnswer(btn4.text.toString(), questionTextView, btn1, btn2, btn3, btn4) }

        startProgressBar(questionTextView)
    }

    private fun displayQuestion(questionTextView: TextView, btn1: Button, btn2: Button, btn3: Button, btn4: Button) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            questionTextView.text = "Date: ${quizItem.date}\n\n${quizItem.question}"
            if (quizItem.choices.size >= 4) {
                btn1.text = quizItem.choices[0]
                btn2.text = quizItem.choices[1]
                btn3.text = quizItem.choices[2]
                btn4.text = quizItem.choices[3]
            } else {
                questionTextView.text = "Error: Not enough options available for this question."
            }
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView, btn1: Button, btn2: Button, btn3: Button, btn4: Button) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            // 정답 판별을 위해 Log로 확인
            Log.d("Quiz", "User Answer: $userAnswer, Correct Answer: ${quizItem.answer}")
            if (userAnswer == quizItem.answer) {
                correctAnswers++
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                SingletonKotlin.updateUserCoins(5, coinText) // Correct answer rewards 5 coins
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
            }
            currentQuestionIndex++
            if (currentQuestionIndex < quizList.size) {
                displayQuestion(questionTextView, btn1, btn2, btn3, btn4)
            } else {
                val totalTime = System.currentTimeMillis() - startTime
                SingletonKotlin.saveGameResult("4 Choice", correctAnswers, totalTime)
                questionTextView.text = "Quiz completed! Correct answers: $correctAnswers, Time taken: ${totalTime / 1000} seconds\nReturning to game selection screen in 3 seconds..."
                handler.postDelayed({
                    finish()
                }, 3000)
            }
        }
    }

    private fun startProgressBar(questionTextView: TextView) {
        Thread {
            while (progressStatus < 300) {  // 0.2 * 300 = 60 seconds
                progressStatus += 1
                handler.post {
                    progressBar.progress = progressStatus
                }
                try {
                    Thread.sleep(200)   // 0.2 seconds delay
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            handler.post {
                questionTextView.text = "Time's up!"
            }
        }.start()
    }
}
