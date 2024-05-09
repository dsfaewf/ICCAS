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

        val imageButton1 = findViewById<View>(R.id.my_button4) as Button
        val imageButton2 = findViewById<View>(R.id.my_button2) as Button

        imageButton1.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity2::class.java)
            startActivity(intent)
        }

        imageButton2.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity3::class.java)
            startActivity(intent)
        }
    }
}
