package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val imageButton = findViewById<View>(R.id.my_button4) as Button
        imageButton.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity2::class.java)
            val intents = arrayOf(intent)
            startActivities(intents)
        }
    }
}
