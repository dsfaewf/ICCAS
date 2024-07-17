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
    private var musicServiceIntent: Intent? = null  // MusicService의 Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyFontSize() // 폰트 크기 적용
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        mediaPlayer = MediaPlayer.create(this, R.raw.paper_flip)

        // MusicService 시작
        musicServiceIntent = Intent(this, MusicService::class.java)
        startService(musicServiceIntent)

        checkFirstLogin()

        val imageButton1 = findViewById<View>(R.id.my_button4) as Button
        val diaryButton = findViewById<View>(R.id.my_button2) as Button
        val catRoomButton = findViewById<View>(R.id.my_button3) as Button
        val gameButton = findViewById<Button>(R.id.btn_game)
//        val gameRecodeBtn = findViewById<View>(R.id.my_button5) as Button
        val photoBtn = findViewById<View>(R.id.my_button6) as Button

        imageButton1.setOnClickListener {
            navigateToSettingUI()
        }

        diaryButton.setOnClickListener {
            mediaPlayer.start()
            navigateToDiaryWriteUI()
        }

        gameButton.setOnClickListener {
            navigateToGameListActivity()
        }

        catRoomButton.setOnClickListener {
            navigateToCatRoomActivity()
        }

//        gameRecodeBtn.setOnClickListener {
//            navigateToOXGameRecordActivity()
//        }
        photoBtn.setOnClickListener {
            navigateToPhotoActivity()
        }
    }

    private fun navigateToSettingUI() {
        val intent = Intent(applicationContext, Setting_UI::class.java)
        startActivity(intent)
    }

    private fun navigateToDiaryWriteUI() {
        val intent = Intent(applicationContext, Diary_write_UI::class.java)
        startActivity(intent)
    }

    private fun navigateToGameListActivity() {
        val intent = Intent(applicationContext, GamelistActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCatRoomActivity() {
        val intent = Intent(applicationContext, CatRoomActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToOXGameRecordActivity() {
        val intent = Intent(applicationContext, OXGameRecordActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPhotoActivity() {
        val intent = Intent(applicationContext, PhotoActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()

        // MusicService 중지
        stopService(musicServiceIntent)
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
            }
            .setNegativeButton("Do it Later", null)
            .show()
    }
}
