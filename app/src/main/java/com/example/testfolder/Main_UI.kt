package com.example.testfolder

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Main_UI : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var mediaPlayer: MediaPlayer   // 효과음 재생용 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyFontSize() // 폰트 크기 적용
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        mediaPlayer = MediaPlayer.create(this, R.raw.paper_flip)

        // MusicService 시작
        startService(Intent(this, MusicService::class.java))

        checkFirstLogin()

        val imageButton1 = findViewById<View>(R.id.my_button4) as Button
        val diaryButton = findViewById<View>(R.id.my_button2) as Button
        val catRoomButton = findViewById<View>(R.id.my_button3) as Button
        val gameButton = findViewById<Button>(R.id.btn_game)
        val gameRecodeBtn = findViewById<View>(R.id.my_button5) as Button

        imageButton1.setOnClickListener {
            val intent = Intent(applicationContext, Setting_UI::class.java)
            startActivity(intent)
            finish()
        }

        diaryButton.setOnClickListener {
            // 효과음 재생 테스트
            mediaPlayer.start()

            // Diary_write_UI 액티비티로 이동
            val intent = Intent(applicationContext, Diary_write_UI::class.java)
            startActivity(intent)
            finish()
        }

        gameButton.setOnClickListener {
            val intent = Intent(this, gamelistActivity::class.java)
            startActivity(intent)
            finish()
        }

        catRoomButton.setOnClickListener {
            val intent = Intent(this, CatRoomActivity::class.java)
            startActivity(intent)
            finish()
        }

        gameRecodeBtn.setOnClickListener {
            val intent = Intent(applicationContext, GameRecordActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()

        // MusicService 중지하지 않음
        // stopService(Intent(this, MusicService::class.java))
    }

    private fun checkFirstLogin() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = database.reference.child("users").child(userId)
            userRef.child("surveyCompleted").get().addOnSuccessListener { dataSnapshot ->
                val surveyCompleted = dataSnapshot.getValue(Boolean::class.java) ?: false
                if (!surveyCompleted) {
                    showSurveyDialog()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to get data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSurveyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Request for a survey")
            .setMessage("Oh? You haven't filled out the survey yet! Please take a brief survey!")
            .setPositiveButton("Go to Survey") { dialog, which ->
                val intent = Intent(this, SurveyActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Do it Later", null)
            .show()
    }
}
