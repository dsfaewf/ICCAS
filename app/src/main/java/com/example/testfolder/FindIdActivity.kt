package com.example.testfolder

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*

class FindIdActivity : AppCompatActivity() {

    private val TAG = "FindIdActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_id)

        val inputEmail = findViewById<TextInputEditText>(R.id.input_email)
        val findIdBtn = findViewById<Button>(R.id.find_id_btn)

        findIdBtn.setOnClickListener {
            val email = inputEmail.text.toString()

            if (email.isNotEmpty()) {
                findUserIdByEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findUserIdByEmail(email: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("users")
        userRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val userId = userSnapshot.child("userId").getValue(String::class.java)
                        if (userId != null) {
                            showUserIdDialog(userId)
                            return
                        }
                    }
                } else {
                    Toast.makeText(this@FindIdActivity, "No user found with this email", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Database error: ${databaseError.message}")
                Toast.makeText(this@FindIdActivity, "Error fetching data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUserIdDialog(userId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Your ID is")
            .setMessage("\" $userId \" ")
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }
}
