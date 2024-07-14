package com.example.testfolder

import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        applyFontSize()
    }

    fun applyFontSize(excludeIds: Set<Int> = emptySet()) {
        val fontSize = sharedPreferences.getFloat("fontSize", 16f)
        setFontSize(window.decorView.rootView, fontSize, excludeIds)
    }

    private fun setFontSize(view: View, fontSize: Float, excludeIds: Set<Int>) {
        if (view.id in excludeIds) {
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setFontSize(view.getChildAt(i), fontSize, excludeIds)
            }
        } else if (view is TextView) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        } else if (view is Button) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            val padding = (fontSize * 2).toInt()
            view.setPadding(padding, padding, padding, padding)
        }
    }
}
