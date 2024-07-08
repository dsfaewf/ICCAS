package com.example.testfolder

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DiaryDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)

        auth = FirebaseAuth.getInstance()
        val diaryId = intent.getStringExtra("diaryId")
        val date = intent.getStringExtra("date")
        val content = intent.getStringExtra("content")

        val dateEditText = findViewById<EditText>(R.id.edit_date)
        val contentEditText = findViewById<EditText>(R.id.edit_content)
        val saveButton = findViewById<Button>(R.id.btn_save)
        val deleteButton = findViewById<Button>(R.id.btn_delete)

        dateEditText.setText(date)
        contentEditText.setText(content)

        saveButton.setOnClickListener {
            val newDate = dateEditText.text.toString()
            val newContent = contentEditText.text.toString()
            if (diaryId != null) {
                updateDiaryEntry(diaryId, newDate, newContent)
            }
        }

        deleteButton.setOnClickListener {
            if (diaryId != null) {
                deleteDiaryEntry(diaryId)
            }
        }
    }

    private fun updateDiaryEntry(diaryId: String, date: String, content: String) { //수정이 가능하도록!
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val diaryRef = FirebaseDatabase.getInstance().reference //db도 함께 수정되어야 함.
                .child("diaries")
                .child(currentUser.uid)
                .child(diaryId)

            val diaryEntryMap = mapOf(
                "date" to date,
                "content" to content
            )

            diaryRef.setValue(diaryEntryMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Diary has been modified.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to modify diary: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteDiaryEntry(diaryId: String) { //삭제가 가능하도록!
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val diaryRef = FirebaseDatabase.getInstance().reference
                .child("diaries")
                .child(currentUser.uid)
                .child(diaryId)

            diaryRef.removeValue() //db에서도 삭제 가능하도록!
                .addOnSuccessListener {
                    Toast.makeText(this, "Diary has been deleted.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to delete diary: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
