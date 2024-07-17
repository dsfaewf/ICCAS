package com.example.testfolder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.util.TypedValue


class GamelistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamelist)

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
        }

        settingBtn.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            builder.setTitle("Main game Setting")
                .setMessage("Choose period")

            val radioGroup = RadioGroup(this)
            val options = arrayOf("Option 1", "Option 2", "Option 3","Option 3")

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
                    dialog.dismiss()
                } else {
                }
            }

            val dialog = builder.create()
            dialog.setCancelable(false)
            dialog.show()

        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, Main_UI::class.java)
        startActivity(intent)
        finish()
    }
}