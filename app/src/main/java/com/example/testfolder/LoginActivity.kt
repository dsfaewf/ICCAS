package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginbtn = findViewById<Button>(R.id.login_btn)
        val findidpage = findViewById<TextView>(R.id.find_id_tv)
        val findpwpage = findViewById<TextView>(R.id.find_pw_tv)
        val registerpage = findViewById<TextView>(R.id.register_tv)

        loginbtn.setOnClickListener {
            val intent = Intent(this, Main_UI::class.java)
            startActivity(intent)
        }

        findidpage.setOnClickListener {
            val intent = Intent(this, FindIdActivity::class.java)
            startActivity(intent)
        }

        findpwpage.setOnClickListener {
            val intent = Intent(this, FindPwActivity::class.java)
            startActivity(intent)
        }
        registerpage.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }
}