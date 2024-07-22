package com.katzheimer.testfolder

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Main_UI : com.katzheimer.testfolder.BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var mediaPlayer: MediaPlayer? = null
    private var musicServiceIntent: Intent? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyFontSize() // 폰트 크기 적용
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isMusicOn = sharedPreferences.getBoolean("music_on", true)

        if (isMusicOn) {
            mediaPlayer = MediaPlayer.create(this, R.raw.paper_flip)
            musicServiceIntent = Intent(this, com.katzheimer.testfolder.MusicService::class.java)
            startService(musicServiceIntent)
            mediaPlayer?.start()
        }

//        checkFirstLogin() //설문 페이지 알림 일단 꺼 놓음.

        val imageButton1 = findViewById<View>(R.id.my_button4) as Button
        val diaryButton = findViewById<View>(R.id.my_button2) as Button
        val catRoomButton = findViewById<View>(R.id.my_button3) as Button
        val gameButton = findViewById<Button>(R.id.btn_game)
        val photoBtn = findViewById<View>(R.id.my_button6) as Button

        imageButton1.setOnClickListener {
            navigateToSettingUI()
        }

        diaryButton.setOnClickListener {
            mediaPlayer?.start()
            navigateToDiaryWriteUI()
        }

        gameButton.setOnClickListener {
            navigateToGameListActivity()
        }

        catRoomButton.setOnClickListener {
            navigateToCatRoomActivity()
        }

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

    private fun navigateToPhotoActivity() {
        val intent = Intent(applicationContext, PhotoActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()

        if (musicServiceIntent != null) {
            stopService(musicServiceIntent)
        }
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
