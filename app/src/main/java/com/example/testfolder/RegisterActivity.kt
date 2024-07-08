package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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
        val textViewError = findViewById<TextView>(R.id.textview_error)

        registerBtn.setOnClickListener {
            val userId = inputId.text.toString().trim() //ID 추가
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val passwordConfirm = inputPasswordConfirm.text.toString().trim()

            if (userId.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                if (password == passwordConfirm) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val firebaseUserId = auth.currentUser?.uid
                                if (firebaseUserId != null) {
                                    addUserToDatabase(firebaseUserId, userId, email) //파라미터 하나 늘어남
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    val error_msg = "Failed: Cannot bring User ID."
                                    textViewError.text = error_msg
                                    textViewError.visibility = TextView.VISIBLE
//                                    Toast.makeText(this, error_msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                try {
                                    throw task.exception!!
                                } catch (e: FirebaseAuthUserCollisionException) {
                                    val error_msg = "The email already exists."
                                    textViewError.text = error_msg
                                    textViewError.visibility = TextView.VISIBLE
//                                    Toast.makeText(this, error_msg, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    val error_msg = "Failed: ${task.exception?.message}"
                                    textViewError.text = error_msg
                                    textViewError.visibility = TextView.VISIBLE
//                                    Toast.makeText(this, error_msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                } else {
                    val error_msg = "Passwords do not match."
                    textViewError.text = error_msg
                    textViewError.visibility = TextView.VISIBLE
//                    Toast.makeText(this, error_msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                val error_msg = "All fields are required."
                textViewError.text = error_msg
                textViewError.visibility = TextView.VISIBLE
//                Toast.makeText(this, error_msg, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Failed to add user information to database: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
