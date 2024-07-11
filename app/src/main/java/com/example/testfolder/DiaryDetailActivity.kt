package com.example.testfolder

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.testfolder.utils.LoadingAnimation
import com.example.testfolder.utils.OpenAI
import com.example.testfolder.utils.PreprocessTexts
import com.example.testfolder.viewmodels.ApiKeyViewModel
import com.example.testfolder.viewmodels.DiaryWriteViewModel
import com.example.testfolder.viewmodels.FirebaseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DiaryDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var newDate: String
    private var numOfQuestions: Int = 0
    private var numOfTokens: Int = 0
    private var lastClickTime: Long = 0
    private val interval: Long = 1000
    private lateinit var newContent: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_detail)

        val apiKeyViewModel = ViewModelProvider(this).get(ApiKeyViewModel::class.java)
        val firebaseViewModel = ViewModelProvider(this).get(FirebaseViewModel::class.java)
        val diaryWriteViewModel = ViewModelProvider(this).get(DiaryWriteViewModel::class.java)
        val myOpenAI = OpenAI(this, apiKeyViewModel, firebaseViewModel)

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.OX_table_deleted.observe(this) {
            Log.d("DB", "Now save the new data")
            myOpenAI.save_OX_data()
        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.MCQ_table_deleted.observe(this) {
            Log.d("DB", "Now save the new data")
            myOpenAI.save_MCQ_data()
        }

        // Once a DB table whose date is the same as selected date is delete
        // Save new data from ChatGPT
        firebaseViewModel.blank_table_deleted.observe(this) {
            Log.d("DB", "Now save the new data")
            myOpenAI.save_blank_quiz_data()
        }

        auth = FirebaseAuth.getInstance()
        val diaryId = intent.getStringExtra("diaryId")
        val date = intent.getStringExtra("date")
        val content = intent.getStringExtra("content")

        val dateTextView = findViewById<TextView>(R.id.edit_date)
        val errorTextView = findViewById<TextView>(R.id.textview_error)
        val contentEditText = findViewById<EditText>(R.id.edit_content)
        val saveButton = findViewById<Button>(R.id.btn_save)
        val deleteButton = findViewById<Button>(R.id.btn_delete)

        dateTextView.text = date
        this.newDate = dateTextView.text.toString().replace("/", " ")
        contentEditText.setText(content)

        saveButton.setOnClickListener {
            // Prevent double click the button
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= interval) {
                lastClickTime = currentTime
                Log.d("SAVE_BUTTON", "Diary save button clicked.")
                // Update class variable with the current diary content
                this.newContent = contentEditText.text.toString().trim()
                this.numOfTokens = PreprocessTexts.getNumOfTokens(newContent)
                this.numOfQuestions = numOfTokens/5
                // If the diary is too short, don't run
                if (numOfQuestions < 1){
                    errorTextView.text = "The diary is too short"
                    errorTextView.visibility = TextView.VISIBLE
                }
                // Run only if the diary is long enough
                else {
                    errorTextView.visibility = TextView.INVISIBLE
                    // Update diary
                    updateDiaryEntry()
                    // Update quiz
                    diaryWriteViewModel.onButtonClick()
                }
            }
        }

        diaryWriteViewModel.buttonClickEvent.observe(this){
            // Update date, a member of OpenAI's instance
            myOpenAI.updateDate(dateTextView.text.toString())
            // Observe the LiveData
            // Once the api key is fetched, generate new 3 types of quiz
            apiKeyViewModel.apiKey.observe(this) {
                myOpenAI.generate_OX_quiz_and_save(
                    this.newContent,
                    this.numOfQuestions)
            }
            myOpenAI.fetchApiKey()
        }

        deleteButton.setOnClickListener {
            if (diaryId != null) {
                deleteDiaryEntry()
                deleteQuizData("ox_quiz")
                deleteQuizData("mcq_quiz")
                deleteQuizData("blank_quiz")
            }
        }
    }

    private fun updateDiaryEntry() { //수정이 가능하도록!
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val diaryRef = FirebaseDatabase.getInstance().reference //db도 함께 수정되어야 함.
                .child("diaries")
                .child(currentUser.uid)
                .child(this.newDate)

            val diaryEntryMap = mapOf(
                "content" to this.newContent
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

    private fun deleteDiaryEntry() { //삭제가 가능하도록!
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val diaryRef = FirebaseDatabase.getInstance().reference
                .child("diaries")
                .child(currentUser.uid)
                .child(this.newDate)

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
    private fun deleteQuizData(tableName: String) { //삭제가 가능하도록!
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val oxQuizRef = FirebaseDatabase.getInstance().reference
                .child(tableName)
                .child(currentUser.uid)
                .child(this.newDate)

            oxQuizRef.removeValue() //db에서도 삭제 가능하도록!
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
