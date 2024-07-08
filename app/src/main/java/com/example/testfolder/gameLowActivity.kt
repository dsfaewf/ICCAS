package com.example.testfolder

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser

class gameLowActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar;
    private var progressStatus = 0;
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_low)
        progressBar = findViewById(R.id.progressBar1)
        var question = findViewById<TextView>(R.id.qeustionbox) // 질문텍스트
        var obutton = findViewById<Button>(R.id.o_btn)  // o 버튼
        var xbutton = findViewById<Button>(R.id.x_btn)  // x 버튼
        coinText = findViewById(R.id.coin_text)
        question.setText("질문 값 넣기 ~~ ") // setText로 문장 설정 간으
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
        obutton.setOnClickListener {
            // o 버튼 눌렀을 때 이벤트
        }

        xbutton.setOnClickListener {
            // x 버튼 눌렀을 때 이벤트
        }

        Thread {    // progressbar 설정
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
            handler.post {  // 60초 넘었을 때,
                question.setText("60초 끝!!1")    // 임시로 글자만 변경
            }
        }.start()

    }
}