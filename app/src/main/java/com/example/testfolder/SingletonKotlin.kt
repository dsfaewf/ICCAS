package com.example.testfolder

import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

object SingletonKotlin {

    private var isInitialized = false
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    fun initialize(auth: FirebaseAuth, database: DatabaseReference) {
        this.auth = auth
        this.database = database
        isInitialized = true
    }

    private fun checkInitialization() {
        if (!isInitialized) {
            throw IllegalStateException("SingletonKotlin is not initialized, call initialize() method first.")
        }
    }

    fun loadUserCoins(coinText: TextView) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)

        userRef.child("coins").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val coins = snapshot.getValue(Long::class.java) ?: 0L
                coinText.text = coins.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(coinText.context, "데이터베이스 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun getAuth(): FirebaseAuth {
        checkInitialization()
        return auth
    }

    fun getDatabase(): DatabaseReference {
        checkInitialization()
        return database
    }

    fun getCurrentUser(): FirebaseUser? {
        checkInitialization()
        return auth.currentUser
    }
}
