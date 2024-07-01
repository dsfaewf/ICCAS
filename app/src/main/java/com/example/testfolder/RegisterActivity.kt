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
        val inputId = findViewById<TextInputEditText>(R.id.input_ID) //ID 필드 추가
        val inputEmail = findViewById<TextInputEditText>(R.id.input_email)
        val inputPassword = findViewById<TextInputEditText>(R.id.input_pw)
        val inputPasswordConfirm = findViewById<TextInputEditText>(R.id.input_pw_confirm)
        val registerBtn = findViewById<Button>(R.id.register_btn)

        registerBtn.setOnClickListener {
            val userId = inputId.text.toString() //ID 추가
            val email = inputEmail.text.toString()
            val password = inputPassword.text.toString()
            val passwordConfirm = inputPasswordConfirm.text.toString()

            if (userId.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                if (password == passwordConfirm) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val firebaseUserId = auth.currentUser?.uid
                                if (firebaseUserId != null) {
                                    addUserToDatabase(firebaseUserId, userId, email) //파라미터 하나 늘어남
                                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "회원가입 실패: 사용자 ID를 가져올 수 없음", Toast.LENGTH_SHORT).show()
                                }
                            } else {
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
                Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addUserToDatabase(firebaseUserId: String, userId: String, email: String) { //파라미터 늘어남
        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(firebaseUserId)
        val userData = hashMapOf(
            "userId" to userId,
            "email" to email
            // 다른 사용자 정보도 추가할 수 있음
        )
        userRef.setValue(userData)
            .addOnSuccessListener {
                // 데이터베이스에 사용자 정보 추가 성공
            }
            .addOnFailureListener { exception ->
                // 데이터베이스에 사용자 정보 추가 실패
                Toast.makeText(this, "데이터베이스에 사용자 정보 추가 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
