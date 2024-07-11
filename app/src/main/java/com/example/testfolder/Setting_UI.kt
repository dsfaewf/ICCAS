package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Setting_UI : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val backButton = findViewById<View>(R.id.back_button) as Button
        val viewGameRecordButton = findViewById<View>(R.id.gameRecord) as Button

        backButton.setOnClickListener {
            val intent = Intent(applicationContext, Main_UI::class.java)
            startActivity(intent)
        }

        viewGameRecordButton.setOnClickListener {
            val intent = Intent(applicationContext, GameRecordActivity::class.java)
            startActivity(intent)
        }
    }
}
