package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class CatRoomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cat_room)

        val coinText = findViewById<TextView>(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        val shopBtn = findViewById<TextView>(R.id.shop_btn)
        val decoBtn = findViewById<TextView>(R.id.deco_btn)
        val diaryBtn = findViewById<TextView>(R.id.diary_btn)
        val gameBtn = findViewById<TextView>(R.id.game_btn)

        coinText.text = "100" // 사용자 보유 코인 수

        // GIF 반복 설정
        val gifDrawable = catGif.drawable as GifDrawable
        gifDrawable.loopCount = 0 // 무한 반복

        shopBtn.setOnClickListener {
            // Shop 버튼 클릭 시 동작
        }
        decoBtn.setOnClickListener {
            // Deco 버튼 클릭 시 동작
        }
        diaryBtn.setOnClickListener {
            val intent = Intent(applicationContext, Diary_write_UI::class.java)
            startActivity(intent)
        }
        gameBtn.setOnClickListener {
            val intent = Intent(applicationContext, gametestActivity::class.java)
            startActivity(intent)
        }
    }
}
