package com.katzheimer.testfolder

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating account...")
        progressDialog.setCancelable(false) // 취소 불가능하도록 설정
        val inputId = findViewById<TextInputEditText>(R.id.input_ID) // ID 필드 추가
        val inputEmail = findViewById<TextInputEditText>(R.id.input_email)
        val inputPassword = findViewById<TextInputEditText>(R.id.input_pw)
        val inputPasswordConfirm = findViewById<TextInputEditText>(R.id.input_pw_confirm)
        val registerBtn = findViewById<Button>(R.id.register_btn)
        val textViewError = findViewById<TextView>(R.id.textview_error)

        registerBtn.setOnClickListener {
            val userId = inputId.text.toString().trim() // ID 추가
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val passwordConfirm = inputPasswordConfirm.text.toString().trim()

            if (userId.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                if (password == passwordConfirm) {
                    progressDialog.show() // ProgressDialog 표시
                    // 사용자 아이디 중복 확인
                    val userRef = FirebaseDatabase.getInstance().reference.child("users")
                    userRef.orderByChild("userId").equalTo(userId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {    //아이디 중복을 일단 확인하고
                                    progressDialog.dismiss() // ProgressDialog 숨기기
                                    val error_msg = "This ID is already taken."
                                    textViewError.text = error_msg
                                    textViewError.visibility = TextView.VISIBLE
                                } else {
                                    // 아이디가 중복되지 않으면 회원가입 진행
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(this@RegisterActivity) { task ->
                                            progressDialog.dismiss() // ProgressDialog 숨기기
                                            if (task.isSuccessful) {
                                                val firebaseUserId = auth.currentUser?.uid
                                                if (firebaseUserId != null) {
                                                    auth.currentUser?.sendEmailVerification()
                                                        ?.addOnCompleteListener { verificationTask ->
                                                            if (verificationTask.isSuccessful) {
                                                                addUserToDatabase(
                                                                    firebaseUserId,
                                                                    userId,
                                                                    email
                                                                ) // 파라미터 하나 늘어남
                                                                Toast.makeText(
                                                                    this@RegisterActivity,
                                                                    "Registration successful. Please check your email for verification.",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                val intent = Intent(
                                                                    this@RegisterActivity,
                                                                    LoginActivity::class.java
                                                                )
                                                                startActivity(intent)
                                                                finish()
                                                            } else {
                                                                val error_msg =
                                                                    "Failed to send verification email: ${verificationTask.exception?.message}"
                                                                textViewError.text = error_msg
                                                                textViewError.visibility =
                                                                    TextView.VISIBLE
                                                            }
                                                        }
                                                } else {
                                                    val error_msg = "Failed: Cannot bring User ID."
                                                    textViewError.text = error_msg
                                                    textViewError.visibility = TextView.VISIBLE
                                                }
                                            } else {
                                                try {
                                                    throw task.exception!!
                                                } catch (e: FirebaseAuthUserCollisionException) {
                                                    val error_msg = "The email already exists."
                                                    textViewError.text = error_msg
                                                    textViewError.visibility = TextView.VISIBLE
                                                } catch (e: Exception) {
                                                    val error_msg =
                                                        "Failed: ${task.exception?.message}"
                                                    textViewError.text = error_msg
                                                    textViewError.visibility = TextView.VISIBLE
                                                }
                                            }
                                        }
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                progressDialog.dismiss() // ProgressDialog 숨기기
                                val error_msg = "Database ERROR: ${databaseError.message}"
                                textViewError.text = error_msg
                                textViewError.visibility = TextView.VISIBLE
                            }
                        })
                } else {
                    val error_msg = "Passwords do not match."
                    textViewError.text = error_msg
                    textViewError.visibility = TextView.VISIBLE
                }
            } else {
                val error_msg = "All fields are required."
                textViewError.text = error_msg
                textViewError.visibility = TextView.VISIBLE
            }
        }
    }


    private fun addUserToDatabase(
        firebaseUserId: String,
        userId: String,
        email: String
    ) { // 파라미터 늘어남
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
                progressDialog.dismiss() // ProgressDialog 숨기기
                // 데이터베이스에 사용자 정보 추가 실패
                Toast.makeText(
                    this,
                    "Failed to add user information to database: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
