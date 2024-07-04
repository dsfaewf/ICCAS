package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class CatRoomActivity : AppCompatActivity() {

    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cat_room)

        try {
            currentUser = SingletonKotlin.getCurrentUser() ?: throw IllegalStateException("사용자 인증이 필요합니다.")
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin이 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        coinText = findViewById(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        val shopBtn = findViewById<TextView>(R.id.shop_btn)
        val decoBtn = findViewById<TextView>(R.id.deco_btn)
        val diaryBtn = findViewById<TextView>(R.id.diary_btn)
        val gameBtn = findViewById<TextView>(R.id.game_btn)

        // 사용자 코인 불러오기
        try {
            SingletonKotlin.loadUserCoins(coinText)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin이 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // GIF 반복 설정
        val gifDrawable = catGif.drawable as GifDrawable
        gifDrawable.loopCount = 0 // 무한 반복

        shopBtn.setOnClickListener {
            val intent = Intent(applicationContext, ShopActivity::class.java)
            startActivity(intent)
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
