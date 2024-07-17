package com.example.testfolder

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ImageListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var imageListAdapter: ImageListAdapter
    private val imageList = mutableListOf<ImageData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)

        listView = findViewById(R.id.imageListView)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val btnSortByDate: Button = findViewById(R.id.btnSortByDate)
        val btnSortByMonth: Button = findViewById(R.id.btnSortByMonth)

        btnSortByDate.setOnClickListener {
            sortImagesByDate()
        }

        btnSortByMonth.setOnClickListener {
            sortImagesByMonth()
        }

        imageListAdapter = ImageListAdapter(this, imageList)
        listView.adapter = imageListAdapter

        loadImagesFromFirebase()
    }

    private fun loadImagesFromFirebase() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userImagesRef = database.child("users").child(uid).child("images")

            userImagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    imageList.clear()
                    for (imageSnapshot in snapshot.children) {
                        val imageUrl = imageSnapshot.child("ImageUrl").value as? String
                        val keyword = imageSnapshot.child("Keyword").value as? String
                        val dateTime = imageSnapshot.child("DateTime").value as? String
                        val gpsLatitude = imageSnapshot.child("GPSLatitude").value as? String
                        val gpsLongitude = imageSnapshot.child("GPSLongitude").value as? String
                        if (imageUrl != null && keyword != null) {
                            val imageData = ImageData(imageUrl, keyword, dateTime, gpsLatitude, gpsLongitude)
                            imageList.add(imageData)
                        }
                    }
                    imageListAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Failed to load images", error.toException())
                }
            })
        }
    }

    private fun sortImagesByDate() {
        imageList.sortBy { it.dateTime }
        imageListAdapter.notifyDataSetChanged()
    }

    private fun sortImagesByMonth() {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        imageList.sortBy {
            it.dateTime?.let { dateStr ->
                dateFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)!!)
            }
        }
        imageListAdapter.notifyDataSetChanged()
    }
}
