package com.example.testfolder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GamelistActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamelist)

        sharedPreferences = getSharedPreferences("game_settings", Context.MODE_PRIVATE)

        val btn1 = findViewById<LinearLayout>(R.id.btn1)
        val btn2 = findViewById<LinearLayout>(R.id.btn2)
        val btn3 = findViewById<LinearLayout>(R.id.btn3)
        val btn4 = findViewById<LinearLayout>(R.id.btn4)
        val btn5 = findViewById<LinearLayout>(R.id.btn5)
        val btn6 = findViewById<LinearLayout>(R.id.btn6)
        val btn7 = findViewById<LinearLayout>(R.id.btn7)
        val gRbtn = findViewById<Button>(R.id.game_record_btn)
        val settingBtn = findViewById<FrameLayout>(R.id.settingButton)

        btn1.setOnClickListener {
            val intent = Intent(this, GameLowActivity::class.java)
            startActivity(intent)
        }
        btn2.setOnClickListener {
            val intent = Intent(this, GameMidActivity::class.java)
            startActivity(intent)
        }
        btn3.setOnClickListener {
            val intent = Intent(this, GameHighActivity::class.java)
            startActivity(intent)
        }
        btn7.setOnClickListener {
            val intent = Intent(this, GamePictureActivity::class.java)
            startActivity(intent)
        }
        btn4.setOnClickListener {
            val intent = Intent(this, MinigameDescriptionNumberActivity::class.java)
            startActivity(intent)
        }
        btn5.setOnClickListener {
            val intent = Intent(this, MinigameDescriptionBaseballActivity::class.java)
            startActivity(intent)
        }
        btn6.setOnClickListener {
            val intent = Intent(this, MinigameDescriptionSamepictureActivity::class.java)
            startActivity(intent)
        }
        gRbtn.setOnClickListener {
            val intent = Intent(this, OXGameRecordActivity::class.java)
            startActivity(intent)
            finish()
        }

        settingBtn.setOnClickListener {
            showSettingDialog()
        }
    }

    private fun showSettingDialog() {
        val builder = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
        builder.setTitle("Main game Setting")
            .setMessage("Choose period")

        val radioGroup = RadioGroup(this)
        val options = arrayOf("Random", "Within 3 days", "3 days to 1 week", "1 week to 2 weeks", "2 weeks to 1 month", "1 month to 6 months")

        val marginInDp = 16
        val marginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, marginInDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        for ((index, option) in options.withIndex()) {
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.id = index
            val params = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            radioButton.layoutParams = params
            radioGroup.addView(radioButton)
        }

        builder.setView(radioGroup)

        builder.setPositiveButton("OK") { dialog, _ ->
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedOption = options[selectedId]
                saveSetting(selectedOption)
                dialog.dismiss()
//                finish()
            }
        }

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun saveSetting(option: String) {
        with(sharedPreferences.edit()) {
            putString("selected_period", option)
            apply()
        }
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
