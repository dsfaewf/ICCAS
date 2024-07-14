package com.example.testfolder

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class FindPwActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_pw)

        auth = FirebaseAuth.getInstance()

        val inputEmail = findViewById<TextInputEditText>(R.id.input_email)
        val resetPwBtn = findViewById<Button>(R.id.reset_pw_btn)

        resetPwBtn.setOnClickListener {
            val email = inputEmail.text.toString()

            if (email.isNotEmpty()) {
                resetPassword(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPassword(email: String) {
        val builder = AlertDialog.Builder(this)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    builder.setTitle("Find Password")
                        .setMessage("Password reset email sent")
                        .setPositiveButton("OK") { dialog, which ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    builder.setTitle("Find Password")
                        .setMessage("Error sending password reset email")
                        .setPositiveButton("OK") { dialog, which ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
    }
}
