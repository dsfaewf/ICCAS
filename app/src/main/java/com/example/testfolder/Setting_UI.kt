package com.example.testfolder

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Setting_UI : BaseActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var sampleTextView: TextView
    private lateinit var backButton: Button
    private lateinit var fetchDiaryButton: Button
    private lateinit var deleteAccountButton: Button
    private lateinit var musicSwitch: Switch

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val currentUser = auth.currentUser
    private val uid = currentUser?.uid
    private var musicServiceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        backButton = findViewById(R.id.back_button)
        fetchDiaryButton = findViewById(R.id.fetch_diary_button)
        deleteAccountButton = findViewById(R.id.delete_account_button)
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar)
        sampleTextView = findViewById(R.id.sampleTextView)
        musicSwitch = findViewById(R.id.music_switch)
        musicServiceIntent = Intent(this, MusicService::class.java)

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
            navigateToMain()
        }

        fetchDiaryButton.setOnClickListener {
            sendDiariesByEmail()
        }

        deleteAccountButton.setOnClickListener {
            confirmDeleteAccount()
        }

        // Load the saved music setting
        val isMusicOn = sharedPreferences.getBoolean("music_on", true)
        musicSwitch.isChecked = isMusicOn

        musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("music_on", isChecked)
            editor.apply()

            if (isChecked) {
                startService(musicServiceIntent)
            } else {
                stopService(musicServiceIntent)
            }
        }
    }

    private fun saveFontSize(size: Float) {
        with(sharedPreferences.edit()) {
            putFloat("fontSize", size)
            apply()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, Main_UI::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }

    private fun sendDiariesByEmail() {
        uid?.let {
            val userDiaryRef = database.reference.child("diaries").child(uid)
            userDiaryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val diaryEntries = StringBuilder()
                    for (diarySnapshot in snapshot.children) {
                        val date = diarySnapshot.key
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        diaryEntries.append("Date: $date\nContent: $content\n\n")
                    }
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "plain/text"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(currentUser?.email))
                        putExtra(Intent.EXTRA_SUBJECT, "Your Diaries")
                        putExtra(Intent.EXTRA_TEXT, diaryEntries.toString())
                    }
                    startActivity(Intent.createChooser(emailIntent, "Send email..."))
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Setting_UI, "Failed to fetch diaries", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun confirmDeleteAccount() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                showFinalConfirmationDialog()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showFinalConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Final Confirmation")
            .setMessage("Your cat will really miss you..")
            .setPositiveButton("Goodbye") { dialog, _ ->
                sendDiariesAndDeleteAccount()
            }
            .setNegativeButton("Stay with us") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun sendDiariesAndDeleteAccount() {
        uid?.let {
            val userDiaryRef = database.reference.child("diaries").child(uid)
            userDiaryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val diaryEntries = StringBuilder()
                    for (diarySnapshot in snapshot.children) {
                        val date = diarySnapshot.key
                        val content = diarySnapshot.child("content").getValue(String::class.java)
                        diaryEntries.append("Date: $date\nContent: $content\n\n")
                    }
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "plain/text"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(currentUser?.email))
                        putExtra(Intent.EXTRA_SUBJECT, "Your Diaries")
                        putExtra(Intent.EXTRA_TEXT, diaryEntries.toString())
                    }
                    startActivity(Intent.createChooser(emailIntent, "Send email..."))

                    deleteAllUserData()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Setting_UI, "Failed to fetch diaries", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun deleteAllUserData() {
        uid?.let {
            val userRef = database.reference.child("users").child(uid)
            val diaryRef = database.reference.child("diaries").child(uid)
            val gameResultsRef = database.reference.child("game_results").child(uid)
            val oxQuizRef = database.reference.child("ox_quiz").child(uid)
            val mcqQuizRef = database.reference.child("mcq_quiz").child(uid)
            val blankQuizRef = database.reference.child("blank_quiz").child(uid)
            val userRoomsRef = database.reference.child("user_rooms").child(uid)
            val userCatFriendsRef = database.reference.child("user_cat_friends").child(uid)

            // Delete all user data
            userRef.removeValue()
            diaryRef.removeValue()
            gameResultsRef.removeValue()
            oxQuizRef.removeValue()
            mcqQuizRef.removeValue()
            blankQuizRef.removeValue()
            userRoomsRef.removeValue()
            userCatFriendsRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        } else {
                            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to delete user data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToMain() // 이전 화면으로 돌아갈 때 메인 화면으로 이동
    }
}
