package com.example.testfolder

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class Diary_write_UI : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        val saveButton = findViewById<Button>(R.id.my_button6)
        val diaryEditText = findViewById<EditText>(R.id.diary_edit_text)
        val characterCountTextView = findViewById<TextView>(R.id.character_count_text_view)
        val dateTextView = findViewById<TextView>(R.id.date_text_view)

        // Firebase 데이터베이스 루트 참조 가져오기
        databaseReference = FirebaseDatabase.getInstance().reference.child("diaries")
        // Firebase 인증 가져오기
        auth = FirebaseAuth.getInstance()

        // Save 버튼 클릭 시 날짜 표시
        saveButton.setOnClickListener {
            // 현재 날짜를 가져오기
            val currentDate = getCurrentDate()
            // 날짜를 TextView에 설정
            dateTextView.text = currentDate

            // 일기 내용을 Firebase 데이터베이스에 업로드
            val diaryContent = diaryEditText.text.toString()
            // 현재 로그인된 사용자의 UID 가져오기
            val currentUser = auth.currentUser
            val userId = currentUser?.uid
            // 사용자별로 데이터 저장하기
            userId?.let {
                val userDiaryRef = databaseReference.child(it)
                // 날짜와 일기 내용 함께 저장
                val diaryEntry = DiaryEntry(currentDate, diaryContent)
                userDiaryRef.push().setValue(diaryEntry)
            }
        }

        // 텍스트 입력 수를 표시
        diaryEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                characterCountTextView.text = "$count/1000"
            }
        })
    }

    // 현재 날짜를 가져오는 함수
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    // 일기 내용과 함께 저장할 데이터 클래스
    data class DiaryEntry(val date: String, val content: String)
}
