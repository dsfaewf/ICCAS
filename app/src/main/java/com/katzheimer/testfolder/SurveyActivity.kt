package com.katzheimer.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SurveyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey) // 여기서 activity_survey 레이아웃 파일을 사용.

        // 싱글톤 초기화
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference
        SingletonKotlin.initialize(auth, database)

        val editTextChildhoodMemory = findViewById<EditText>(R.id.editTextChildhoodMemory)
        val editTextFavoriteSubject = findViewById<EditText>(R.id.editTextFavoriteSubject)
        val editTextHobby = findViewById<EditText>(R.id.editTextHobby)
        val editTextMemorableTrip = findViewById<EditText>(R.id.editTextMemorableTrip)
        val editTextChildhoodDream = findViewById<EditText>(R.id.editTextChildhoodDream)
        val editTextProudestMoment = findViewById<EditText>(R.id.editTextProudestMoment)
        val submitBtn = findViewById<Button>(R.id.submitBtn)

        submitBtn.setOnClickListener {
            val childhoodMemory = editTextChildhoodMemory.text.toString()
            val favoriteSubject = editTextFavoriteSubject.text.toString()
            val hobby = editTextHobby.text.toString()
            val memorableTrip = editTextMemorableTrip.text.toString()
            val childhoodDream = editTextChildhoodDream.text.toString()
            val proudestMoment = editTextProudestMoment.text.toString()

            val userSurvey = hashMapOf(             //내용들 변경
                "childhoodMemory" to childhoodMemory,
                "favoriteSubject" to favoriteSubject,
                "hobby" to hobby,
                "memorableTrip" to memorableTrip,
                "childhoodDream" to childhoodDream,
                "proudestMoment" to proudestMoment
            )

            val userId = SingletonKotlin.getCurrentUser()?.uid
            if (userId != null) {
                val userRef = SingletonKotlin.getDatabase().child("surveys").child(userId)
                userRef.setValue(userSurvey)
                    .addOnSuccessListener {
                        val userMetaRef = SingletonKotlin.getDatabase().child("users").child(userId)
                        userMetaRef.child("surveyCompleted").setValue(true)
                        Toast.makeText(this, "Survey saved successfully.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SurveyActivity, Main_UI::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save survey: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Failed to get user ID.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
