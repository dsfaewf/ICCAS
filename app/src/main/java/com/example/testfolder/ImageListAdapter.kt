package com.example.testfolder

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.ActionCodeUrl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale

data class ImageData(
    val imageid: String,
    val imageUrl: String,
    val imgName: String,
    val keyword: String?,
    val dateTime: String?,
    val gpsLatitude: String?,
    val gpsLongitude: String?
)

class ImageListAdapter(private val context: Context, private val imageList: MutableList<ImageData>) : BaseAdapter() {
    private lateinit var auth: FirebaseAuth

    var deletedPhoto = false
    var deletedPhotoQuiz = false

    override fun getCount(): Int {
        return imageList.size
    }

    override fun getItem(position: Int): Any {
        return imageList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.image_list_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val imageData = imageList[position]
        holder.keywordTextView.text = imageData.keyword
        holder.dateTimeTextView.text = imageData.dateTime
        holder.imageView.clipToOutline = true
        holder.deleteButton.setOnClickListener {
            imageData.dateTime?.let { it1 -> deleteQuizData("img_quiz", it1, imageData.imageUrl) }
            deletePhotoEntry(imageData.imageid)
            deleteImageStorage(imageData.imgName)
            imageList.removeAt(position)
            notifyDataSetChanged()
        }

        Glide.with(context)
            .load(imageData.imageUrl)
            .centerCrop()
            .into(holder.imageView)

        return view
    }

    private class ViewHolder(view: View) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val keywordTextView: TextView = view.findViewById(R.id.keywordTextView)
        val dateTimeTextView: TextView = view.findViewById(R.id.dateTimeTextView)
        val deleteButton: TextView = view.findViewById(R.id.deletePhoto)
    }

    private fun deletePhotoEntry(id: String) {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val photoRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUser.uid)
                .child("images")
                .child(id)

            photoRef.removeValue()
                .addOnSuccessListener {
                    deletedPhoto = true
                    if (deletedPhoto && deletedPhotoQuiz) {
                        Toast.makeText(context, "Photo has been deleted.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to delete diary: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteImageStorage(imgName: String) {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val storageRef = FirebaseStorage.getInstance().getReference("images/${currentUser.uid}/$imgName")

            Log.d("DeleteImageStorage", "Attempting to delete image at path: images/${currentUser.uid}/$imgName")

            storageRef.metadata.addOnSuccessListener {
                // Log successful metadata retrieval
                Log.d("DeleteImageStorage", "Image exists, proceeding with deletion.")
                storageRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("DeleteImageStorage", "Failed to delete image: ${exception.message}")
                        Toast.makeText(context, "Failed to delete image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                // Log the image not existing
                Log.e("DeleteImageStorage", "Image does not exist: ${it.message}")
                Toast.makeText(context, "Image does not exist: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteQuizData(tableName: String, dateTime: String, imageUrl: String) {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val year = dateTime.substring(0,4)
        val month = dateTime.substring(5,7)
        val day = dateTime.substring(8,10)
        val newDate = "$day $month $year"
        Log.d("newDate", newDate)
        if (currentUser != null) {
            val oxQuizRef = FirebaseDatabase.getInstance().reference
                .child(tableName)
                .child(currentUser.uid)
                .child(newDate)

            oxQuizRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var deleteTargetId: String? = null
                    for (imgQuizSnapshot in snapshot.children) {
                        val urlInfo = imgQuizSnapshot.child("url").getValue(String::class.java)
//                        Log.d("urlInfo", "urlInfo: $urlInfo")
                        if (urlInfo.equals(imageUrl)) {
                            deleteTargetId = imgQuizSnapshot.key
                        }
                        if (deleteTargetId != null) {
                            oxQuizRef.child(deleteTargetId).removeValue()
                                .addOnSuccessListener {
                                    Log.d("DeleteData", "Data with URL $imageUrl deleted successfully.")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("DeleteData", "Failed to delete data: ${exception.message}")
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // 데이터 읽기 실패 시 처리합니다.
                }
            })
        }
    }
}
