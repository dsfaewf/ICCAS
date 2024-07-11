package com.example.testfolder

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.testfolder.utils.LoadingAnimation
import com.example.testfolder.utils.OpenAI
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.example.testfolder.utils.PreprocessTexts
import com.example.testfolder.viewmodels.ApiKeyViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testfolder.viewmodels.DiaryWriteViewModel
import com.example.testfolder.viewmodels.FirebaseViewModel
import io.ktor.util.date.toDate
import java.text.SimpleDateFormat

class Diary_write_UI : AppCompatActivity() {
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화
    val currentUser = auth.currentUser
    val uid = currentUser?.uid // 현재 로그인된 사용자의 UID 가져오기
    private val interval: Long = 1000
    private var lastClickTime: Long = 0
    private var numOfQuestions: Int = 0
    private var numOfTokens: Int = 0
    private lateinit var diaryContent: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKeyViewModel = ViewModelProvider(this).get(ApiKeyViewModel::class.java)
        val firebaseViewModel = ViewModelProvider(this).get(FirebaseViewModel::class.java)
        val diaryWriteViewModel = ViewModelProvider(this).get(DiaryWriteViewModel::class.java)

        setContentView(R.layout.activity_write_diary)

        val saveButton = findViewById<Button>(R.id.my_button6)
        val backButton1 = findViewById<Button>(R.id.my_button8)
        val diarycheckButton = findViewById<Button>(R.id.my_button7)
        val diaryEditText = findViewById<EditText>(R.id.diary_edit_text)
        val characterCountTextView = findViewById<TextView>(R.id.character_count_text_view)
        val dateTextView = findViewById<TextView>(R.id.date_text_view)
        val errorTextView = findViewById<TextView>(R.id.textview_error)
        val loadingBackgroundLayout = findViewById<ConstraintLayout>(R.id.loading_background_layout)
        val loadingImage = findViewById<ImageView>(R.id.loading_image)
        val loadingText = findViewById<TextView>(R.id.loading_text)
        val calendarBtn = findViewById<ImageButton>(R.id.calBtn)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Initialize date
        dateTextView.text = getCurrentDate()

        Log.d("Open AI", "Open AI Class is being created")
        val myOpenAI = OpenAI(this, apiKeyViewModel, firebaseViewModel)

//        // UI 상 date가 바뀔 때마다 동작하는 함수
//        diaryWriteViewModel.liveDataDate.observe(this) {
//            myOpenAI.updateDate(dateTextView.text.toString())
//        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.OX_table_deleted.observe(this) {
            myOpenAI.save_OX_data()
        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.MCQ_table_deleted.observe(this) {
            myOpenAI.save_MCQ_data()
        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.blank_table_deleted.observe(this) {
            myOpenAI.save_blank_quiz_data()
        }

        // Save 버튼 클릭 시 날짜 표시 및 일기 내용 저장
        saveButton.setOnClickListener {
            // Prevent double click the button
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= interval) {
                lastClickTime = currentTime
                Log.d("SAVE_BUTTON", "Diary save button clicked.")
                // Update class variable with the current diary content
                this.diaryContent = diaryEditText.text.toString().trim()
                this.numOfTokens = PreprocessTexts.getNumOfTokens(diaryContent)
                this.numOfQuestions = numOfTokens/5
                // If the diary is too short, don't run
                if (numOfQuestions < 1){
                    errorTextView.text = "The diary is too short"
                    errorTextView.visibility = TextView.VISIBLE
                }
                // Run only if the diary is long enough
                else {
                    errorTextView.visibility = TextView.INVISIBLE
                    diaryWriteViewModel.onButtonClick()
                }
            }
        }

        // calendar Button 클릭 시
        calendarBtn.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day
                ->
                // 날짜를 TextView에 설정
                dateTextView.text = String.format("%02d/%02d/%04d", day, month+1, year)
            }, year, month, day)
            datePickerDialog.show()
        }

        diaryWriteViewModel.buttonClickEvent.observe(this){
            val loadingAnimation = LoadingAnimation(this,
                loadingBackgroundLayout, loadingImage, loadingText)

//            ViewModel에 있는 date 값을 담고있는 LiveData를 update
//            diaryWriteViewModel.setDate(dateTextView.text.toString())

            // EditText 내용 Clear
            diaryEditText.text.clear()
            Log.d("Date", dateTextView.text.toString())
            // OpenAI 인스턴스의 date도 함께 update
            myOpenAI.updateDate(dateTextView.text.toString())

            // 일기 내용을 Firebase 데이터베이스에 업로드
            // 사용자별로 데이터 저장하기
            uid?.let {
                val date = dateTextView.text.toString().replace("/", " ")
                val userDiaryRef = database.reference
                    .child("diaries")
                    .child(uid)
                    .child(date) // 사용자별 레퍼런스 생성
                val diaryEntryMap = mapOf(
                    "content" to diaryContent
                )
                val dbTask = userDiaryRef.setValue(diaryEntryMap) // 사용자별 위치에 일기 저장
                dbTask.addOnSuccessListener {
                    Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }

            // TEST
            if (uid != null) {
                // Observe the LiveData
                apiKeyViewModel.apiKey.observe(this) {
                    myOpenAI.generate_OX_quiz_and_save(
                        this.diaryContent,
                        this.numOfQuestions)
                }
                myOpenAI.fetchApiKey()
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
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    // change String to Date object
    private fun stringToDateObject(stringDate: String): Date {
        val (day, month, year) = stringDate.split("/").map { it -> it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.MONTH, month - 1) // Month is 0-based in Calendar
            set(Calendar.YEAR, year)
        }
        val specificDate = calendar.time
        return specificDate
    }
}