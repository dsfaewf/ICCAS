package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testfolder.utils.OpenAI
import com.example.testfolder.utils.PreprocessTexts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class Diary_write_UI : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    //private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_diary)

        val saveButton = findViewById<Button>(R.id.my_button6)
        val backButton1 = findViewById<Button>(R.id.my_button8)
        val diarycheckButton = findViewById<Button>(R.id.my_button7)
        val diaryEditText = findViewById<EditText>(R.id.diary_edit_text)
        val characterCountTextView = findViewById<TextView>(R.id.character_count_text_view)
        val dateTextView = findViewById<TextView>(R.id.date_text_view)
        val errorTextView = findViewById<TextView>(R.id.textview_error)

//        // Firebase 데이터베이스 루트 참조 가져오기
//        databaseReference = FirebaseDatabase.getInstance().reference.child("diaries")
//        auth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화
//        database = FirebaseDatabase.getInstance()
        // SingletonKotlin을 통해 Firebase 객체를 불러오도록 수정함
        auth = SingletonKotlin.getAuth()
        databaseReference = SingletonKotlin.getDatabase().child("diaries")

        // Save 버튼 클릭 시 날짜 표시 및 일기 내용 저장
        saveButton.setOnClickListener {
            // 현재 날짜를 가져오기
            val currentDate = getCurrentDate()
            // 날짜를 TextView에 설정
            dateTextView.text = currentDate

            // 일기 내용을 Firebase 데이터베이스에 업로드
            val diaryContent = diaryEditText.text.toString().trim()
            // 현재 로그인된 사용자의 UID 가져오기
            val currentUser = auth.currentUser
            val userId = currentUser?.uid
            // 사용자별로 데이터 저장하기
            userId?.let {
                val userDiaryRef = databaseReference.child(it) // 사용자별 레퍼런스 생성
                val diaryEntryMap = mapOf(
                    "date" to currentDate,
                    "content" to diaryContent
                )
                userDiaryRef.push().setValue(diaryEntryMap) // 사용자별 위치에 일기 저장
            }

            // TEST
            if (userId != null) {
                val numOfTokens = PreprocessTexts.getNumOfTokens(diaryContent)
                val numOfQuestions = numOfTokens/5
                if (numOfQuestions < 1){
                    errorTextView.text = "The diary is too short"
                    errorTextView.visibility = TextView.VISIBLE
                }
                else {
                    errorTextView.text = ""
                    errorTextView.visibility = TextView.INVISIBLE
                    val myOpenAI = OpenAI()
                    myOpenAI.generate_OX_quiz_and_save(diaryContent, numOfQuestions, dateTextView.text.toString())
                }

            } else {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
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

        // Back 버튼 클릭 시 메인 화면으로 이동
        backButton1.setOnClickListener {
            val intent = Intent(applicationContext, Main_UI::class.java)
            startActivity(intent)
        }

        // 일지조회 버튼 클릭 시 Diary_Check 화면으로 이동
        diarycheckButton.setOnClickListener {
            val intent = Intent(applicationContext, Diary_Check::class.java)
            startActivity(intent)
        }
    }

    // 현재 날짜를 가져오는 함수
    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
}