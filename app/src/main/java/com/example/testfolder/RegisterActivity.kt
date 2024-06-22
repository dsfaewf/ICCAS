package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val inputEmail = findViewById<TextInputEditText>(R.id.input_email)
        val inputPassword = findViewById<TextInputEditText>(R.id.input_pw)
        val inputPasswordConfirm = findViewById<TextInputEditText>(R.id.input_pw_confirm)
        val registerBtn = findViewById<Button>(R.id.register_btn)

        registerBtn.setOnClickListener {
            val email = inputEmail.text.toString()
            val password = inputPassword.text.toString()
            val passwordConfirm = inputPasswordConfirm.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                if (password == passwordConfirm) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    // 데이터베이스에 사용자 정보 추가
                                    addUserToDatabase(userId, email)

                                    // 회원가입이 성공한 경우 메시지 출력
                                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()

                                    // LoginActivity로 이동
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "회원가입 실패: 사용자 ID를 가져올 수 없음", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // 예외 처리 추가
                                try {
                                    throw task.exception!!
                                } catch (e: FirebaseAuthUserCollisionException) {
                                    Toast.makeText(this, "회원가입 실패: 이미 존재하는 이메일입니다.", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                } else {
                    Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addUserToDatabase(userId: String, email: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        val userData = hashMapOf(
            "email" to email
            // 다른 사용자 정보도 추가할 수 있음
        )
        userRef.setValue(userData)
            .addOnSuccessListener {
                // 데이터베이스에 사용자 정보 추가 성공
                // 여기서 다른 작업을 수행할 수 있음
            }
            .addOnFailureListener { exception ->
                // 데이터베이스에 사용자 정보 추가 실패
                Toast.makeText(this, "데이터베이스에 사용자 정보 추가 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
