package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CatRoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cat_room)

        var coinText = findViewById<TextView>(R.id.coin_text)

        val shopBth = findViewById<TextView>(R.id.shop_btn)
        val decoBtn = findViewById<TextView>(R.id.deco_btn)
        val diaryBtn = findViewById<TextView>(R.id.diary_btn)
        val gameBtn = findViewById<TextView>(R.id.game_btn)

        coinText.setText("100") // 사용자 보유 코인 수

        shopBth.setOnClickListener {

        }
        decoBtn.setOnClickListener {

        }
        diaryBtn.setOnClickListener {
            val intent = Intent(applicationContext, Diary_write_UI::class.java)
            startActivity(intent)
        }
        gameBtn.setOnClickListener {
            // game 화면 확인 필요
            val intent = Intent(applicationContext, gametestActivity::class.java)
            startActivity(intent)
        }

    }
}