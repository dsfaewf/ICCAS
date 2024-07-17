package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Diary_Check : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var diaryContainer: LinearLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var diaryListAdapter: DiaryListAdapter
    private val diaryList = mutableListOf<DiaryData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_check)
        applyFontSize() // 폰트 크기 적용

        listView = findViewById(R.id.diaryListView)
        diaryListAdapter = DiaryListAdapter(this, diaryList)
        listView.adapter = diaryListAdapter

        // Singleton을 통해 Firebase 객체 갖고 오게 변경
        auth = SingletonKotlin.getAuth()
        val currentUser = SingletonKotlin.getCurrentUser()

        if (currentUser != null) {
            databaseReference = SingletonKotlin.getDatabase()
                .child("diaries")
                .child(currentUser.uid)

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
//                    diaryContainer.removeAllViews()
                    diaryList.clear()
//                    for (diarySnapshot in snapshot.children) {
//                        var date = diarySnapshot.key
//                        date = date?.replace(" ", "/")
//                        Log.d("DIARY_LIST", "date: $date")
//                        val content = diarySnapshot.child("content").getValue(String::class.java)
//                        val diaryId = diarySnapshot.key
//                        if (!date.isNullOrEmpty() && !content.isNullOrEmpty() && !diaryId.isNullOrEmpty()) {
//                            addDiaryEntryToView(date, content, diaryId)
//                        }
//                    }
                    for (diarySnapshot in snapshot.children) {
                        val date = diarySnapshot.key?.replace(" ", "/")
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        val diaryId = diarySnapshot.key
                        if (!date.isNullOrEmpty() && !content.isNullOrEmpty() && !diaryId.isNullOrEmpty()) {
                            diaryList.add(DiaryData(diaryId, content, date))
                        }
                    }
                    diaryListAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val diaryData = diaryList[position]
            val intent = Intent(this@Diary_Check, DiaryDetailActivity::class.java).apply {
                putExtra("diaryId", diaryData.diaryId)
                putExtra("date", diaryData.date)
                putExtra("content", diaryData.content)
            }
            startActivity(intent)
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
    private fun navigateToDiaryWriteUI() {
        val intent = Intent(this, Diary_write_UI::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToDiaryWriteUI() // 이전 화면으로 돌아갈 때 Diary_write_UI로 이동
    }
}