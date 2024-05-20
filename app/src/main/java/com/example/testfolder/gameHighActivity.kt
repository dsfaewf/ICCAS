package com.example.testfolder

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class gameHighActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar;
    private var progressStatus = 0;
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_high)
        progressBar = findViewById(R.id.progressBar1)
        var question = findViewById<TextView>(R.id.qeustionbox)
        var submitBtn = findViewById<Button>(R.id.submit_btn)

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