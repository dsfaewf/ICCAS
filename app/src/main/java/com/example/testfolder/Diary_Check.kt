package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

        // Singleton을 통해 Firebase 객체 갖고 오게 변경
        auth = SingletonKotlin.getAuth()
        val currentUser = SingletonKotlin.getCurrentUser()

        if (currentUser != null) {
            databaseReference = SingletonKotlin.getDatabase()
                .child("diaries")
                .child(currentUser.uid)

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    diaryContainer.removeAllViews()
                    for (diarySnapshot in snapshot.children) {
                        var date = diarySnapshot.key
                        date = date?.replace(" ", "/")
                        Log.d("DIARY_LIST", "date: $date")
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        val diaryId = diarySnapshot.key
                        if (!date.isNullOrEmpty() && !content.isNullOrEmpty() && !diaryId.isNullOrEmpty()) {
                            addDiaryEntryToView(date, content, diaryId)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
        }
    }

    private fun addDiaryEntryToView(date: String, content: String, diaryId: String) {
        val dateTextView = TextView(this).apply {
            text = date
            textSize = 18f
            setPadding(0, 10, 0, 0)
            setTextColor(resources.getColor(android.R.color.black)) // Text color set to black
        }

        val contentTextView = TextView(this).apply {
            text = content
            textSize = 16f
            setPadding(0, 5, 0, 10)
            setTextColor(resources.getColor(android.R.color.black)) // Text color set to black
        }

        val diaryEntryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 20)
            addView(dateTextView)
            addView(contentTextView)
            //setBackgroundResource(R.drawable.diary_entry_background) // Optional: add background to each entry
            setOnClickListener {
                val intent = Intent(this@Diary_Check, DiaryDetailActivity::class.java)
                intent.putExtra("diaryId", diaryId)
                intent.putExtra("date", date)
                intent.putExtra("content", content)
                startActivity(intent)
            }
        }

        diaryContainer.addView(diaryEntryLayout)
    }
}
