package com.example.testfolder

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseUser

class gameHighActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar;
    private var progressStatus = 0;
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_high)
        progressBar = findViewById(R.id.progressBar1)
        var question = findViewById<TextView>(R.id.qeustionbox)
        var submitBtn = findViewById<Button>(R.id.submit_btn)
        coinText = findViewById(R.id.coin_text)

        try {
            currentUser = SingletonKotlin.getCurrentUser() ?: throw IllegalStateException("사용자 인증이 필요합니다.")
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin이 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // 사용자 코인 불러오기
        try {
            SingletonKotlin.loadUserCoins(coinText)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin이 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        question.setText("질문 값 넣기 ~~ ")

        Thread {
            while (progressStatus < 300) {  // 0.2 * 300 = 60초
                progressStatus += 1
                handler.post {
                    progressBar.progress = progressStatus
                }
                try {
                    Thread.sleep(200)   // 0.2초 대기
                } catch (e:InterruptedException) {
                    e.printStackTrace()
                }
            }
            handler.post { // 60초 넘었을 때,
                question.setText("60초 끝!!1")
            }
        }.start()

        submitBtn.setOnClickListener {
            // 정답 확인 코드
        }
    }
}