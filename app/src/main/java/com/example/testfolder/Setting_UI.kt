package com.example.testfolder

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView

class Setting_UI : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var sampleTextView: TextView
    private lateinit var backButton: Button
//    private lateinit var viewGameRecordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        backButton = findViewById(R.id.back_button)
//        viewGameRecordButton = findViewById(R.id.gameRecord)
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar)
        sampleTextView = findViewById(R.id.sampleTextView)

        val savedFontSize = sharedPreferences.getFloat("fontSize", 16f)
        fontSizeSeekBar.max = 24  // Maximum increase from the minimum size
        fontSizeSeekBar.progress = (savedFontSize - 16).toInt()
        sampleTextView.textSize = savedFontSize

        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newSize = 16f + progress
                sampleTextView.textSize = newSize
                saveFontSize(newSize)
                applyFontSize() // 폰트 크기 적용
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        backButton.setOnClickListener {
            val intent = Intent(applicationContext, Main_UI::class.java)
            startActivity(intent)
        }

//        viewGameRecordButton.setOnClickListener {
//            val intent = Intent(applicationContext, GameRecordActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }

    private fun saveFontSize(size: Float) {
        with(sharedPreferences.edit()) {
            putFloat("fontSize", size)
            apply()
        }
    }
}
