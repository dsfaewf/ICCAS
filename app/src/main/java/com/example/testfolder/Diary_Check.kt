package com.example.testfolder

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Diary_Check : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var diaryContainer: LinearLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_check)

        diaryContainer = findViewById(R.id.diary_container)
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().reference
                .child("diaries")
                .child(currentUser.uid)

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    diaryContainer.removeAllViews()
                    for (diarySnapshot in snapshot.children) {
                        val date = diarySnapshot.child("date").getValue(String::class.java)
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        if (!date.isNullOrEmpty() && !content.isNullOrEmpty()) {
                            addDiaryEntryToView(date, content)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
        }
    }

    private fun addDiaryEntryToView(date: String, content: String) {
        val dateTextView = TextView(this).apply {
            text = date
            textSize = 18f
            setPadding(0, 10, 0, 0)
        }

        val contentTextView = TextView(this).apply {
            text = content
            textSize = 16f
            setPadding(0, 5, 0, 10)
        }

        diaryContainer.addView(dateTextView)
        diaryContainer.addView(contentTextView)
    }
}