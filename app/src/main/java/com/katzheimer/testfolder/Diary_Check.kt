package com.katzheimer.testfolder

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

class Diary_Check : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var diaryListAdapter: DiaryListAdapter
    private lateinit var dateTxt: TextView
    private lateinit var calBtn: ImageButton
    private lateinit var allBtn: Button
    private val diaryList = mutableListOf<DiaryData>()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_check)
        applyFontSize() // 폰트 크기 적용

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        listView = findViewById(R.id.diaryListView)
        dateTxt = findViewById(R.id.date_text_view2)
        calBtn = findViewById(R.id.calBtn2)
        allBtn = findViewById(R.id.button3)
        diaryListAdapter = DiaryListAdapter(this, diaryList)
        listView.adapter = diaryListAdapter

        getData()

        listView.setOnItemClickListener { _, _, position, _ ->
            val diaryData = diaryList[position]
//            val intent = Intent(this@Diary_Check, DiaryDetailActivity::class.java).apply {
//                putExtra("diaryId", diaryData.diaryId)
//                putExtra("date", diaryData.date)
//                putExtra("content", diaryData.content)
//            }
            val intent = Intent(this@Diary_Check, DiaryDetailActivity::class.java).apply {
                putExtra("diaryId", diaryData.diaryId)
                putExtra("date", diaryData.date)
                putExtra("content", diaryData.content)
            }
            startActivity(intent)
        }

        calBtn.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                // 날짜를 TextView에 설정
                dateTxt.text = String.format("%02d/%04d", month + 1, year)

                // 클릭한 날짜로 조회
                databaseReference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        diaryList.clear()

                        for (diarySnapshot in snapshot.children) {
                            val date = diarySnapshot.key?.replace(" ", "/")
                            val content = diarySnapshot.child("content").getValue(String::class.java)
                            val diaryId = diarySnapshot.key

                            if (date != null && content != null && diaryId != null) {
                                // Checking if the year and month match
                                if (date.substring(3) == dateTxt.text.toString()) {
                                    diaryList.add(DiaryData(diaryId, content, date))
                                }
                            }
                        }
                        diaryList.sortBy { it.date?.substring(0, 2) } // 초기 화면 월별로 정렬
                        diaryListAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle possible errors
                    }
                })
            }, year, month, day)
            datePickerDialog.show()
        }

        allBtn.setOnClickListener {
            getData()
            dateTxt.setText(" month / year ")
        }

    }
    private fun navigateToDiaryWriteUI() {
        val intent = Intent(this, Diary_write_UI::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }

    private fun getData() {
        // Initialize if it's not initialized
        if (!SingletonKotlin.isInitialized()) {
            SingletonKotlin.initialize(auth, database)
        }
        val currentUser = SingletonKotlin.getCurrentUser()

        if (currentUser != null) {
            databaseReference = SingletonKotlin.getDatabase()
                .child("diaries")
                .child(currentUser.uid)

            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    diaryList.clear()

                    for (diarySnapshot in snapshot.children) {
                        val date = diarySnapshot.key?.replace(" ", "/")
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        val diaryId = diarySnapshot.key
                        if (!date.isNullOrEmpty() && !content.isNullOrEmpty() && !diaryId.isNullOrEmpty()) {
                            diaryList.add(DiaryData(diaryId, content, date))
                        }
                    }
                    diaryList.sortBy { it.date?.substring(3, 5) }    // 초기 화면 월별로 정렬
                    diaryListAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToDiaryWriteUI() // 이전 화면으로 돌아갈 때 Diary_write_UI로 이동
    }

}