package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SurveyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val radioGroupGender = findViewById<RadioGroup>(R.id.radioGroupGender)
        val editTextBirthYear = findViewById<EditText>(R.id.editTextBirthYear)
        val editTextBirthPlace = findViewById<EditText>(R.id.editTextBirthPlace)
        val radioGroupAlzheimer = findViewById<RadioGroup>(R.id.radioGroupAlzheimer)
        val spinnerEducation = findViewById<Spinner>(R.id.spinnerEducation)
        val editTextMotherBirthYear = findViewById<EditText>(R.id.editTextMotherBirthYear)
        val editTextFatherBirthYear = findViewById<EditText>(R.id.editTextFatherBirthYear)
        val radioGroupSiblings = findViewById<RadioGroup>(R.id.radioGroupSiblings)
        val spinnerResidence = findViewById<Spinner>(R.id.spinnerResidence)
        val radioGroupGeneticTest = findViewById<RadioGroup>(R.id.radioGroupGeneticTest)
        val submitBtn = findViewById<Button>(R.id.submitBtn)

        submitBtn.setOnClickListener {
            val gender = when (radioGroupGender.checkedRadioButtonId) {
                R.id.radioMale -> "남성"
                R.id.radioFemale -> "여성"
                else -> "기타"
            }
            val birthYear = editTextBirthYear.text.toString()
            val birthPlace = editTextBirthPlace.text.toString()
            val alzheimer = when (radioGroupAlzheimer.checkedRadioButtonId) {
                R.id.radioAlzheimerYes -> "예"
                R.id.radioAlzheimerNo -> "아니오"
                else -> "모르겠음"
            }
            val education = spinnerEducation.selectedItem.toString()
            val motherBirthYear = editTextMotherBirthYear.text.toString()
            val fatherBirthYear = editTextFatherBirthYear.text.toString()
            val siblings = when (radioGroupSiblings.checkedRadioButtonId) {
                R.id.radioSiblingsYes -> "예"
                else -> "아니오"
            }
            val residence = spinnerResidence.selectedItem.toString()
            val geneticTest = when (radioGroupGeneticTest.checkedRadioButtonId) {
                R.id.radioGeneticTestYes -> "예"
                else -> "아니오"
            }

            val userSurvey = hashMapOf(
                "gender" to gender,
                "birthYear" to birthYear,
                "birthPlace" to birthPlace,
                "alzheimer" to alzheimer,
                "education" to education,
                "motherBirthYear" to motherBirthYear,
                "fatherBirthYear" to fatherBirthYear,
                "siblings" to siblings,
                "residence" to residence,
                "geneticTest" to geneticTest
            )

            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userRef = database.reference.child("surveys").child(userId)
                userRef.setValue(userSurvey)
                    .addOnSuccessListener {
                        val userMetaRef = database.reference.child("users").child(userId)
                        userMetaRef.child("surveyCompleted").setValue(true)
                        Toast.makeText(this, "설문이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SurveyActivity, Main_UI::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "설문 저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "사용자 ID를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
