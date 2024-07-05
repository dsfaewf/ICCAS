package com.example.testfolder

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class gamelistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamelist)

        val btn1 = findViewById<LinearLayout>(R.id.btn1)
        val btn2 = findViewById<LinearLayout>(R.id.btn2)
        val btn3 = findViewById<LinearLayout>(R.id.btn3)
        val btn4 = findViewById<LinearLayout>(R.id.btn4)
        val btn5 = findViewById<LinearLayout>(R.id.btn5)
        val btn6 = findViewById<LinearLayout>(R.id.btn6)

        btn1.setOnClickListener {
            val intent = Intent(this, gameLowActivity::class.java)
            startActivity(intent)
        }
        btn2.setOnClickListener {
            val intent = Intent(this, gameMidActivity::class.java)
            startActivity(intent)
        }
        btn3.setOnClickListener {
            val intent = Intent(this, gameHighActivity::class.java)
            startActivity(intent)
        }
        btn4.setOnClickListener {
            val intent = Intent(this, ServeGame_NumberActivity::class.java)
            startActivity(intent)
        }
        btn5.setOnClickListener {
            val intent = Intent(this, ServeGameBaseballActivity::class.java)
            startActivity(intent)
        }
        btn6.setOnClickListener {
            val intent = Intent(this, ServeGame_samepicture::class.java)
            startActivity(intent)
        }

    }
}