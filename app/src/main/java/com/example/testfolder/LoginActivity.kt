package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val loginBtn = findViewById<Button>(R.id.login_btn)
        val findIdPage = findViewById<TextView>(R.id.find_id_tv)
        val findPwPage = findViewById<TextView>(R.id.find_pw_tv)
        val registerPage = findViewById<TextView>(R.id.register_tv)

        // 로그인 버튼 클릭 시
        loginBtn.setOnClickListener {
            val email = findViewById<TextView>(R.id.input_id).text.toString()
            val password = findViewById<TextView>(R.id.input_pw).text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                checkUserExistsAndLogin(userId)
                            } else {
                                Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMessage = when (task.exception) {
                                is FirebaseAuthInvalidUserException ->
                                    "등록되지 않은 계정입니다."

                                is FirebaseAuthInvalidCredentialsException ->
                                    "잘못된 ID 또는 Password입니다."

                                else ->
                                    "로그인에 실패했습니다."
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "ID와 Password를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 아이디 찾기 페이지로 이동
        findIdPage.setOnClickListener {
            val intent = Intent(this, FindIdActivity::class.java)
            startActivity(intent)
        }

        // 비밀번호 찾기 페이지로 이동
        findPwPage.setOnClickListener {
            val intent = Intent(this, FindPwActivity::class.java)
            startActivity(intent)
        }

        // 회원가입 페이지로 이동
        registerPage.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 사용자가 존재하는지 확인하고 로그인 처리
    private fun checkUserExistsAndLogin(userId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // 회원가입을 마친 사용자인 경우 로그인 성공
                val intent = Intent(this, Main_UI::class.java)
                startActivity(intent)
                finish()
            } else {
                // 회원가입을 마치지 않은 사용자인 경우
                Toast.makeText(this, "회원가입을 마치신 후 이용해주세요.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "데이터베이스 접근 실패. 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
