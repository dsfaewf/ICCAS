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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GamePictureActivity : BaseActivity() {
    private lateinit var progressBar: ProgressBar
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var quizList: List<QuizItem>
    private lateinit var selectedQuizzes: List<QuizItem>
    private var currentRound = 0
    private var correctAnswers = 0
    private var startTime: Long = 0
    private var totalTime = 0L
    private val roundTime = 60 * 1000 // 60초
    private var progressBarThread: Thread? = null
    private val incorrectQuestions = mutableListOf<Pair<Int, String>>()

    private lateinit var roundImageView: ImageView
    private lateinit var numberImageView: ImageView
    private lateinit var finishedTextView: TextView

    private lateinit var loadingLayout: View
    private lateinit var loadingImage: ImageView
    private lateinit var loadingText: TextView
    private lateinit var questionImg: ImageView

    private lateinit var obutton: FrameLayout
    private lateinit var xbutton: FrameLayout

    private lateinit var getCoin: MediaPlayer
    private lateinit var wrong: MediaPlayer

    private lateinit var correctWrongOverlay: FrameLayout
    private lateinit var correctWrongImage: ImageView

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    data class QuizItem(
        val answer: String = "",
        val question: String = "",
        val date: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_picture)

        sharedPreferences = getSharedPreferences("game_settings", Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        progressBar = findViewById(R.id.progressBar1)
        obutton = findViewById(R.id.o_btn)
        xbutton = findViewById(R.id.x_btn)
        coinText = findViewById(R.id.coin_text)

        roundImageView = findViewById(R.id.roundImageView)
        numberImageView = findViewById(R.id.numberImageView)
        finishedTextView = findViewById(R.id.finishedTextView)

        loadingLayout = findViewById(R.id.loading_layout)
        loadingImage = findViewById(R.id.loading_image)
        loadingText = findViewById(R.id.loading_text)
        questionImg = findViewById(R.id.qeustionImg)

        correctWrongOverlay = findViewById(R.id.correct_wrong_overlay)
        correctWrongImage = findViewById(R.id.correct_wrong_image)

        getCoin = MediaPlayer.create(this, R.raw.coin)
        wrong = MediaPlayer.create(this, R.raw.wrong)
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loadingImage.startAnimation(rotateAnimation)
        startLoadingTextAnimation()

        questionImg.clipToOutline = true

        obutton.isEnabled = false
        xbutton.isEnabled = false

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

        val excludeIds = setOf(R.id.o_btn, R.id.x_btn)
        applyFontSize(excludeIds)

        handler.postDelayed({
            loadingLayout.visibility = View.GONE
            loadQuizData()
        }, 3000)

        obutton.setOnClickListener {
            handleAnswer("O")
        }

        xbutton.setOnClickListener {
            handleAnswer("X")
        }
    }

    private fun loadQuizData() {
        val userId = currentUser.uid
        val quizRef = database.reference.child("img_quiz").child(userId)

        quizRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quizzes = mutableListOf<QuizItem>()
                for (dateSnapshot in snapshot.children) {
                    val date = dateSnapshot.key ?: ""
                    for (quizSnapshot in dateSnapshot.children) {
                        val quiz = quizSnapshot.getValue(QuizItem::class.java)
                        if (quiz != null) {
                            quizzes.add(quiz.copy(date = date))
                        }
                    }
                }
                quizList = quizzes
                if (quizList.size >= 5) {
                    selectedQuizzes = quizList.shuffled().take(5)
                    startRound()
                    obutton.isEnabled = true
                    xbutton.isEnabled = true
                } else {
                    showNoQuizzesDialogAndExit()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GamePictureActivity, "Failed to load quizzes", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showNoQuizzesDialogAndExit() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Quizzes Available")
            .setMessage("There are no quiz entries available.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun startRound() {
        startTime = System.currentTimeMillis()
        progressStatus = 0
        progressBar.progress = progressStatus
        updateRoundImages()
        displayQuestion()
        startProgressBar()
    }

    private fun displayQuestion() {
        if (currentRound < selectedQuizzes.size) {
            val quizItem = selectedQuizzes[currentRound]
            findViewById<TextView>(R.id.qeustionbox).text = "Date: ${quizItem.date}\n\n${quizItem.question}"
            loadImageForQuiz(quizItem)
        } else {
            val totalGameTime = totalTime / 1000
            SingletonKotlin.saveGameResult("OX", correctAnswers, totalGameTime)
            showGameResultDialog(correctAnswers, totalGameTime, incorrectQuestions)
        }
    }

    private fun loadImageForQuiz(quizItem: QuizItem) {
        val userId = currentUser.uid
        val imageRef = database.reference.child("users").child(userId).child("images")

        imageRef.orderByChild("Keyword").equalTo(quizItem.question).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (imageSnapshot in snapshot.children) {
                        val imageUrl = imageSnapshot.child("ImageUrl").getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this@GamePictureActivity).load(imageUrl).into(questionImg)
                            return
                        }
                    }
                }
                // 기본 이미지 설정
                questionImg.setImageResource(R.drawable.colosseum)
            }

            override fun onCancelled(error: DatabaseError) {
                questionImg.setImageResource(R.drawable.colosseum)
            }
        })
    }

    private fun handleAnswer(userAnswer: String) {
        if (currentRound < selectedQuizzes.size) {
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
                incorrectQuestions.add(Pair(currentRound + 1, quizItem.date))
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
            startRound()
        }, 2000)
    }

    private fun startProgressBar() {
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
                    findViewById<TextView>(R.id.qeustionbox).text = "Time's up!"
                    currentRound++
                    startRound()
                }
            }
        }.apply { start() }
    }

    private fun updateRoundImages() {
        val roundDrawable = R.drawable.round
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

    private fun startLoadingTextAnimation() {
        var dotCount = 0
        handler.post(object : Runnable {
            override fun run() {
                dotCount++
                if (dotCount > 3) {
                    dotCount = 0
                }
                val dots = ".".repeat(dotCount)
                loadingText.text = "Pulling out the diary$dots"
                handler.postDelayed(this, 500)
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
                finish()
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
