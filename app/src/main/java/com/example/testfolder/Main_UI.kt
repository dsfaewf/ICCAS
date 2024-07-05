package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Main_UI : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        checkFirstLogin()

        val imageButton1 = findViewById<View>(R.id.my_button4) as Button
        val diaryButton = findViewById<View>(R.id.my_button2) as Button
        val catRoomButton = findViewById<View>(R.id.my_button3) as Button
        val gameButton = findViewById<Button>(R.id.btn_game)

        imageButton1.setOnClickListener {
            val intent = Intent(applicationContext, Setting_UI::class.java)
            startActivity(intent)
        }

        diaryButton.setOnClickListener {
            val intent = Intent(applicationContext, Diary_write_UI::class.java)
            startActivity(intent)
        }

        gameButton.setOnClickListener {
            val intent = Intent(this, gamelistActivity::class.java)
            startActivity(intent)
        }

        catRoomButton.setOnClickListener {
            val intent = Intent(this, CatRoomActivity::class.java)
            startActivity(intent)
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
                Toast.makeText(this, "데이터를 가져오는 데 실패했습니다: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSurveyDialog() {
        AlertDialog.Builder(this)
            .setTitle("설문조사 요청")
            .setMessage("어? 저희 어플 사용이 처음이시네요. 간단한 설문에 응해주세요!")
            .setPositiveButton("설문 응하기") { dialog, which ->
                val intent = Intent(this, SurveyActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("나중에 하기", null)
            .show()
    }
}
