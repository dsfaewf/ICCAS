package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = Firebase.auth

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val inputName = findViewById<TextInputEditText>(R.id.input_name)
        val inputEmail = findViewById<TextInputEditText>(R.id.input_email)
        val inputPassword = findViewById<TextInputEditText>(R.id.input_pw)
        val registerBtn = findViewById<Button>(R.id.register_btn)

        registerBtn.setOnClickListener {
            // 회원가입 로직을 여기에 추가
            // 여기에 회원가입 로직을 구현
            // 회원가입이 성공하면 LoginActivity로 돌아가도록 한다.
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // 현재 RegisterActivity 종료

            auth.createUserWithEmailAndPassword(inputEmail.text.toString(),  inputPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this,"Success", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this,"Failed", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
