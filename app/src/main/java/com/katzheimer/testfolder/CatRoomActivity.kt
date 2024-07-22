package com.katzheimer.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseUser
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class CatRoomActivity : AppCompatActivity() {

    private lateinit var coinText: TextView
    private lateinit var currentUser: FirebaseUser
    private lateinit var roomframe: FrameLayout
    private lateinit var newcatGif: GifImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cat_room)

        try {
            currentUser = SingletonKotlin.getCurrentUser() ?: throw IllegalStateException("User authentication required.")
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin is not initialized.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        coinText = findViewById(R.id.coin_text)

        val shopBtn = findViewById<TextView>(R.id.shop_btn)
        val decoBtn = findViewById<TextView>(R.id.deco_btn)
        val diaryBtn = findViewById<TextView>(R.id.diary_btn)
        val gameBtn = findViewById<TextView>(R.id.game_btn)
        roomframe = findViewById(R.id.room_frame)

        // 기본 배경 설정 -> 유저가 저장한 배경이 있으면 해당 배경으로 없으면 기본 배경으로
        SingletonKotlin.loadUserBackground(roomframe)

        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        newcatGif = findViewById(R.id.newcat_gif)

        // 유저가 구입한 새로운 고양이 친구를 불러오기
        SingletonKotlin.loadUserCatFriend(newcatGif)

        // 사용자 코인 불러오기
        try {
            SingletonKotlin.loadUserCoins(coinText)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "SingletonKotlin is not initialized", Toast.LENGTH_SHORT).show()
            finish()
        }

        // GIF 반복 설정
        val gifDrawable = catGif.drawable as GifDrawable
        gifDrawable.loopCount = 0 // 무한 반복

        shopBtn.setOnClickListener {
            val intent = Intent(applicationContext, ShopActivity::class.java)
            startActivity(intent)
            finish()
        }
        decoBtn.setOnClickListener {
            val intent = Intent(applicationContext, DecoActivity::class.java)
            startActivity(intent)
            finish()
        }
        diaryBtn.setOnClickListener {
            val intent = Intent(applicationContext, Diary_write_UI::class.java)
            startActivity(intent)
            finish()
        }
        gameBtn.setOnClickListener {
            val intent = Intent(applicationContext, GamelistActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun navigateToMain() {
        val intent = Intent(this, Main_UI::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToMain()
//        val intent = Intent(applicationContext, Main_UI::class.java)
//        startActivity(intent)
//        finish()
    }
}
