package com.example.testfolder

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.media.MediaPlayer
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
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.testfolder.utils.LoadingAnimation
import com.example.testfolder.utils.OpenAI
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.example.testfolder.utils.PreprocessTexts
import com.example.testfolder.viewmodels.OpenAIViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testfolder.viewmodels.DiaryWriteViewModel
import com.example.testfolder.viewmodels.FirebaseViewModel
import com.google.firebase.functions.FirebaseFunctions
import java.text.SimpleDateFormat

class Diary_write_UI : BaseActivity() {
    companion object {
        const val PROB_TYPE_OX  = 0
        const val PROB_TYPE_MCQ = 1
        const val PROB_TYPE_BLANK  = 2
        const val PROB_TYPE_HINT  = 3
        const val PROB_TYPE_MCQ_REFINED = 4
        const val PROB_TYPE_IMAGE = 5
        const val NULL_STRING = "NULL"
    }
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화
    val currentUser = auth.currentUser
    val uid = currentUser?.uid // 현재 로그인된 사용자의 UID 가져오기
    private val interval: Long = 1000
    private var lastClickTime: Long = 0
    private var numOfQuestions: Int = 0
    private var numOfTokens: Int = 0
    private lateinit var diaryContent: String
    private lateinit var mediasave: MediaPlayer   //효과음 재생용 변수
    private lateinit var mediafail: MediaPlayer   //효과음 재생용 변수


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val openAIViewModel = ViewModelProvider(this).get(OpenAIViewModel::class.java)
        val firebaseViewModel = ViewModelProvider(this).get(FirebaseViewModel::class.java)
        val diaryWriteViewModel = ViewModelProvider(this).get(DiaryWriteViewModel::class.java)
        applyFontSize() // 폰트 크기 적용
        setContentView(R.layout.activity_write_diary)

        val saveButton = findViewById<Button>(R.id.my_button6)
        val diarycheckButton = findViewById<Button>(R.id.my_button7)
        val diaryEditText = findViewById<EditText>(R.id.diary_edit_text)
        val characterCountTextView = findViewById<TextView>(R.id.character_count_text_view)
        val dateTextView = findViewById<TextView>(R.id.date_text_view)
        val errorTextView = findViewById<TextView>(R.id.textview_error)
        val loadingBackgroundLayout = findViewById<ConstraintLayout>(R.id.loading_background_layout)
        val loadingImage = findViewById<ImageView>(R.id.loading_image)
        val loadingText = findViewById<TextView>(R.id.loading_text)
        val loadingTextDetail = findViewById<TextView>(R.id.loading_text_detail)
        val loadingTextDetail2 = findViewById<TextView>(R.id.loading_text_detail2)
        val calendarBtn = findViewById<ImageButton>(R.id.calBtn)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        mediasave = MediaPlayer.create(this, R.raw.book_close)
        mediafail = MediaPlayer.create(this, R.raw.ding)
        // Initialize date
        dateTextView.text = getCurrentDate()

        val loadingAnimation = LoadingAnimation(this,
            loadingBackgroundLayout, loadingImage, loadingText, loadingTextDetail, loadingTextDetail2, "Please wait")
        Log.d("Open AI", "Open AI Class is being created")
        val myOpenAI = OpenAI(this, this, openAIViewModel, firebaseViewModel, loadingAnimation, diaryEditText)
        myOpenAI.fetchApiKey()

        // Attach observers
        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.OX_table_deleted.observe(this) {
            Log.i("DB", "OX_table_deleted Observer triggered")
            myOpenAI.save_OX_data()
//            firebaseViewModel.OX_table_deleted.removeObservers(this)
        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.MCQ_table_deleted.observe(this) {
            Log.i("DB", "MCQ_table_deleted Observer triggered")
            myOpenAI.save_MCQ_data()
//            firebaseViewModel.MCQ_table_deleted.removeObservers(this)
        }

        openAIViewModel.gotResponseForBlankLiveData.observe(this) {
            Log.i("DB", "gotResponseForBlankLiveData Observer triggered")
            myOpenAI.generate_hint()
//            openAIViewModel.gotResponseForBlankLiveData.removeObservers(this)
        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.blank_table_deleted.observe(this) {
            Log.i("DB", "blank_table_deleted Observer triggered")
            myOpenAI.save_blank_quiz_data()
//            firebaseViewModel.blank_table_deleted.removeObservers(this)
        }

        diaryWriteViewModel.buttonClickEvent.observe(this) {
            Log.i("DB", "buttonClickEvent Observer triggered")
            loadingAnimation.showLoading()

//            ViewModel에 있는 date 값을 담고있는 LiveData를 update
//            diaryWriteViewModel.setDate(dateTextView.text.toString())

            // EditText 내용 Clear
            diaryEditText.text.clear()
            Log.d("Date", dateTextView.text.toString())
            // OpenAI 인스턴스의 date도 함께 update
            myOpenAI.updateDate(dateTextView.text.toString())

            // Call firebase functions
            val requestBodyOX = hashMapOf(
                "model" to myOpenAI.model,
                "userMsg" to myOpenAI.getUserPrompt(
                    probType= PROB_TYPE_OX,
                    diary= this.diaryContent,
                    numOfQuestions= this.numOfQuestions),
                "sysMsg" to myOpenAI.getSysPrompt(
                    probType= PROB_TYPE_OX),
                "uid" to uid,
                "date" to myOpenAI.getDate(),
            )
            val requestBodyMCQ = hashMapOf(
                "model" to myOpenAI.model,
                "userMsg" to myOpenAI.getUserPrompt(
                    probType= PROB_TYPE_MCQ,
                    diary= this.diaryContent,
                    numOfQuestions= this.numOfQuestions),
                "sysMsg" to myOpenAI.getSysPrompt(
                    probType= PROB_TYPE_MCQ),
                "diary" to this.diaryContent,
                "uid" to uid,
                "date" to myOpenAI.getDate(),
            )
            val requestBodyBlank = hashMapOf(
                "model" to myOpenAI.model,
                "userMsg" to myOpenAI.getUserPrompt(
                    probType= PROB_TYPE_BLANK,
                    diary= this.diaryContent,
                    numOfQuestions= this.numOfQuestions),
                "sysMsg" to myOpenAI.getSysPrompt(
                    probType= PROB_TYPE_BLANK),
                "diary" to this.diaryContent,
                "uid" to uid,
                "date" to myOpenAI.getDate(),
            )
            val functions = FirebaseFunctions.getInstance()
            Log.d("Firebase", "Called firebase function for gpt use")
            functions
                .getHttpsCallable("callChatGPTAndStoreResponseAboutOX")
                .call(requestBodyOX)
            functions
                .getHttpsCallable("callChatGPTAndStoreResponseAboutMCQ")
                .call(requestBodyMCQ)
            functions
                .getHttpsCallable("callChatGPTAndStoreResponseAboutBlank")
                .call(requestBodyBlank)

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
                    loadingAnimation.hideLoading()
                    SingletonKotlin.initialize(auth, database.reference)
                    // 코인 업데이트 로직 추가
                    SingletonKotlin.updateUserCoinsWithoutTextView(10,this)
                    Toast.makeText(this, "Diary saved and 10 coins added!", Toast.LENGTH_SHORT).show()
                    Log.i("DB", "Diary saved successfully")
                }
            }


//            // TEST
//            if (uid != null) {
//                myOpenAI.generate_quiz_and_save(this.diaryContent, this.numOfQuestions)
//            } else {
//                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
//            }
//            diaryWriteViewModel.buttonClickEvent.removeObservers(this)
        }
//        // UI 상 date가 바뀔 때마다 동작하는 함수
//        diaryWriteViewModel.liveDataDate.observe(this) {
//            myOpenAI.updateDate(dateTextView.text.toString())
//            diaryWriteViewModel.liveDataDate.removeObservers(this)
//        }

//        firebaseViewModel.hint_table_deleted.observe(this) {
//            myOpenAI.save_hint_data()
//            firebaseViewModel.hint_table_deleted.removeObservers(this)
//        }

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
                this.numOfQuestions = numOfTokens/8
                // If the diary is too short, don't run
                if (numOfQuestions < 1){
                    errorTextView.text = "The diary is too short"
                    mediafail.start()
                    errorTextView.visibility = TextView.VISIBLE
                }
                // Run only if the diary is long enough
                else {
                    mediasave.start() //효과음 재생 추가 - 우석
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

        // 텍스트 입력 수를 표시
        diaryEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                characterCountTextView.text = "$count/1000"
            }
        })

        // 일지조회 버튼 클릭 시 Diary_Check 화면으로 이동
        diarycheckButton.setOnClickListener {
            val intent = Intent(applicationContext, Diary_Check::class.java)
            startActivity(intent)
            finish()
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

    private fun navigateToMain() {
        val intent = Intent(this, Main_UI::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToMain()
//        val intent = Intent(applicationContext, Main_UI::class.java)
//        startActivity(intent)
//        finish()
    }


}