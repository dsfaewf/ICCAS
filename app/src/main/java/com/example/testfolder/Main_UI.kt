package com.example.testfolder

import com.example.testfolder.Diary_write_UI
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class Main_UI : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageButton1 = findViewById<View>(R.id.my_button4) as Button
        val diaryButton = findViewById<View>(R.id.my_button2) as Button

        imageButton1.setOnClickListener {
            val intent = Intent(applicationContext, Setting_UI::class.java)
            startActivity(intent)
        }

        diaryButton.setOnClickListener {
            val intent = Intent(applicationContext, Diary_write_UI::class.java)
            startActivity(intent)
        }
    }
}
