package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class GamelistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamelist)

        val btn1 = findViewById<LinearLayout>(R.id.btn1)
        val btn2 = findViewById<LinearLayout>(R.id.btn2)
        val btn3 = findViewById<LinearLayout>(R.id.btn3)
        val btn4 = findViewById<LinearLayout>(R.id.btn4)
        val btn5 = findViewById<LinearLayout>(R.id.btn5)
        val btn6 = findViewById<LinearLayout>(R.id.btn6)
        val btn7 = findViewById<LinearLayout>(R.id.btn7)
        val gRbtn = findViewById<Button>(R.id.game_record_btn)

        btn1.setOnClickListener {
            val intent = Intent(this, GameLowActivity::class.java)
            startActivity(intent)
        }
        btn2.setOnClickListener {
            val intent = Intent(this, GameMidActivity::class.java)
            startActivity(intent)
        }
        btn3.setOnClickListener {
            val intent = Intent(this, GameHighActivity::class.java)
            startActivity(intent)
        }
        btn7.setOnClickListener {
            val intent = Intent(this, GamePictureActivity::class.java)
            startActivity(intent)
        }
        btn4.setOnClickListener {
            val intent = Intent(this, MinigameDescriptionNumberActivity::class.java)
            startActivity(intent)
        }
        btn5.setOnClickListener {
            val intent = Intent(this, MinigameDescriptionBaseballActivity::class.java)
            startActivity(intent)
        }
        btn6.setOnClickListener {
            val intent = Intent(this, MinigameDescriptionSamepictureActivity::class.java)
            startActivity(intent)
        }
        gRbtn.setOnClickListener {
            val intent = Intent(this, OXGameRecordActivity::class.java)
            startActivity(intent)
        }

    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, Main_UI::class.java)
        startActivity(intent)
        finish()
    }
}