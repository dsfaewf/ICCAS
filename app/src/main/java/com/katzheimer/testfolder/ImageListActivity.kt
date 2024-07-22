package com.katzheimer.testfolder

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ImageListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var imageListAdapter: ImageListAdapter
    private lateinit var dateTxt: TextView
    private lateinit var calBtn: ImageButton
    private lateinit var allBtn: Button
    private val imageList = mutableListOf<ImageData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        listView = findViewById(R.id.imageListView)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        dateTxt = findViewById(R.id.date_text_view2)
        calBtn = findViewById(R.id.calBtn2)
        allBtn = findViewById(R.id.button3)

        imageListAdapter = ImageListAdapter(this, imageList)
        listView.adapter = imageListAdapter

        loadImagesFromFirebase(null)

        calBtn.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                // 날짜를 TextView에 설정
                dateTxt.text = String.format("%02d/%04d", month + 1, year)
                loadImagesFromFirebase(dateTxt.text as String)
            }, year, month, day)
            datePickerDialog.show()
        }
        allBtn.setOnClickListener {
            loadImagesFromFirebase(null)
            dateTxt.setText(" month / year ")
        }
    }

    private fun loadImagesFromFirebase(dateString: String?) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userImagesRef = database.child("users").child(uid).child("images")

            userImagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    imageList.clear()
                    for (imageSnapshot in snapshot.children) {
                        val imageid = imageSnapshot.key
                        val imageUrl = imageSnapshot.child("ImageUrl").value as? String
                        val imgName = imageSnapshot.child("imgName").value as? String
                        val keyword = imageSnapshot.child("Keyword").value as? String
                        val dateTime = imageSnapshot.child("DateTime").value as? String
                        val gpsLatitude = imageSnapshot.child("GPSLatitude").value as? String
                        val gpsLongitude = imageSnapshot.child("GPSLongitude").value as? String
                        val dateFormat = dateTime?.substring(5,7) + "/" + dateTime?.substring(0, 4)
                        Log.d("dateFormat",dateFormat)
                        if (dateString != null) {
                            Log.d("dateString",dateString)
                        }
                        if (imageUrl != null && keyword != null && imgName != null) {
                            if (dateTime != null) {
                                if ( dateFormat == dateString) {
                                    val imageData = imageid?.let { ImageData(it, imageUrl, imgName, keyword, dateTime, gpsLatitude, gpsLongitude) }
                                    if (imageData != null) {
                                        imageList.add(imageData)
                                    }
                                } else if(dateString == null){
                                    val imageData = imageid?.let { ImageData(it, imageUrl, imgName, keyword, dateTime, gpsLatitude, gpsLongitude) }
                                    if (imageData != null) {
                                        imageList.add(imageData)
                                    }
                                }
                            }
                        }
                    }
                    imageList.sortBy { it.dateTime?.substring(5, 7) } // 초기 화면 월별로 정렬
                    imageListAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Failed to load images", error.toException())
                }
            })
        }
    }
}
