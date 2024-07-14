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
    private lateinit var quizList: List<SingletonKotlin.BlankQuizItem>
    private var currentQuestionIndex = 0
    private var correctAnswers = 0
    private var startTime = 0L
    private var totalTime = 0L
    private val totalRounds = 5
    private val roundTime = 60 * 1000 // 60초
    private var progressBarThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_high)
        progressBar = findViewById(R.id.progressBar1)
        val questionTextView = findViewById<TextView>(R.id.qeustionbox)
        val answerEditText = findViewById<EditText>(R.id.answer_edit_text)
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
                startProgressBar(questionTextView, answerEditText)
            } else {
                questionTextView.text = "No quiz available."
            }
        }

        submitBtn.setOnClickListener {
            handleAnswer(answerEditText.text.toString(), questionTextView, answerEditText)
        }
    }

    private fun displayQuestion(questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            val answerLength = quizItem.answer.length
            val question = quizItem.question.replace("<blank>", "_".repeat(answerLength))

            questionTextView.text = "Date: ${quizItem.date}\n\n${question}"
            answerEditText.text.clear()
        } else {
            endQuiz(questionTextView)
        }
    }

    private fun handleAnswer(userAnswer: String, questionTextView: TextView, answerEditText: EditText) {
        if (currentQuestionIndex < quizList.size) {
            val quizItem = quizList[currentQuestionIndex]
            Log.d("Quiz", "User Answer: $userAnswer, Correct Answer: ${quizItem.answer}")
            if (userAnswer.trim().equals(quizItem.answer.trim(), ignoreCase = true)) {
                correctAnswers++
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
                SingletonKotlin.updateUserCoins(5, coinText)
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
            }
            totalTime += System.currentTimeMillis() - startTime
            currentQuestionIndex++
            if (currentQuestionIndex < quizList.size) {
                startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                displayQuestion(questionTextView, answerEditText)
                startProgressBar(questionTextView, answerEditText)
            } else {
                endQuiz(questionTextView)
            }
        }
    }

    private fun endQuiz(questionTextView: TextView) {
        val totalTimeSeconds = totalTime / 1000 // 초 단위로 변환
        SingletonKotlin.saveGameResult("Short Answer", correctAnswers, totalTimeSeconds)
        questionTextView.text = "Quiz completed! Correct answers: $correctAnswers, Total time: ${totalTimeSeconds} seconds\nReturning to game selection screen in 5 seconds..."
        handler.postDelayed({
            finish()
        }, 5000)
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
                    totalTime += roundTime
                    currentQuestionIndex++
                    if (currentQuestionIndex < quizList.size) {
                        startTime = System.currentTimeMillis() // 새로운 라운드 시작 시간 설정
                        displayQuestion(questionTextView, answerEditText)
                        startProgressBar(questionTextView, answerEditText)
                    } else {
                        endQuiz(questionTextView)
                    }
                }
            }
        }.apply { start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressBarThread?.interrupt()
    }
}
