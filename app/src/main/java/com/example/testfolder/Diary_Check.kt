package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Diary_Check : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var diaryContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var searchBar: EditText
    private lateinit var auth: FirebaseAuth
    private val diaryEntries = mutableListOf<DiaryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_check)

        diaryContainer = findViewById(R.id.diary_container)
        scrollView = findViewById(R.id.scroll_view)
        searchBar = findViewById(R.id.search_bar)

        auth = SingletonKotlin.getAuth()
        val currentUser = SingletonKotlin.getCurrentUser()

        if (currentUser != null) {
            databaseReference = SingletonKotlin.getDatabase()
                .child("diaries")
                .child(currentUser.uid)

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    diaryContainer.removeAllViews()
                    diaryEntries.clear()
                    for (diarySnapshot in snapshot.children) {
                        var date = diarySnapshot.key
                        date = date?.replace(" ", "/")
                        Log.d("DIARY_LIST", "date: $date")
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        val diaryId = diarySnapshot.key
                        if (!date.isNullOrEmpty() && !content.isNullOrEmpty() && !diaryId.isNullOrEmpty()) {
                            val diaryEntry = DiaryEntry(date, content, diaryId)
                            diaryEntries.add(diaryEntry)
                            addDiaryEntryToView(diaryEntry)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDiaryEntries(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterDiaryEntries(query: String) {
        diaryContainer.removeAllViews()
        val filteredEntries = diaryEntries.filter {
            it.date.contains(query, ignoreCase = true)
        }
        filteredEntries.forEach { addDiaryEntryToView(it) }
        if (filteredEntries.isNotEmpty()) {
            scrollToDiaryEntry(filteredEntries.first().date)
        }
    }

    private fun scrollToDiaryEntry(date: String) {
        for (i in 0 until diaryContainer.childCount) {
            val diaryEntryView = diaryContainer.getChildAt(i) as LinearLayout
            val dateTextView = diaryEntryView.getChildAt(0) as TextView
            if (dateTextView.text == date) {
                scrollView.post {
                    scrollView.scrollTo(0, diaryEntryView.top)
                }
                break
            }
        }
    }

    private fun addDiaryEntryToView(diaryEntry: DiaryEntry) {
        val dateTextView = TextView(this).apply {
            text = diaryEntry.date
            textSize = 18f
            setPadding(0, 10, 0, 0)
            setTextColor(resources.getColor(android.R.color.black)) // Text color set to black
        }

        val contentTextView = TextView(this).apply {
            text = diaryEntry.content
            textSize = 16f
            setPadding(0, 5, 0, 10)
            setTextColor(resources.getColor(android.R.color.black)) // Text color set to black
        }

        val diaryEntryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 20)
            addView(dateTextView)
            addView(contentTextView)
            setOnClickListener {
                val intent = Intent(this@Diary_Check, DiaryDetailActivity::class.java)
                intent.putExtra("diaryId", diaryEntry.diaryId)
                intent.putExtra("date", diaryEntry.date)
                intent.putExtra("content", diaryEntry.content)
                startActivity(intent)
            }
        }

        diaryContainer.addView(diaryEntryLayout)
    }

    data class DiaryEntry(val date: String, val content: String, val diaryId: String)
}
