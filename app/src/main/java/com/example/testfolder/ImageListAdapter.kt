package com.example.testfolder

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.values
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

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
            deleteImage(imageData.imgName)
            deletePhotoEntry(imageData.imageid)
            deleteQuizData("img_quiz", imageData.imageid)
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
            val diaryRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUser.uid)
                .child("images")
                .child(id)

            diaryRef.removeValue()
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
    private fun deleteImage(imgName: String) {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val storageRef =
                FirebaseStorage.getInstance().getReference("images/${currentUser.uid}/$imgName")


            storageRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        context,
                        "Failed to delete image: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun deleteQuizData(tableName: String, id: String) {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val oxQuizRef = FirebaseDatabase.getInstance().reference
                .child(tableName)
                .child(currentUser.uid)
                .child(id)

            oxQuizRef.removeValue()
                .addOnSuccessListener {
                    deletedPhotoQuiz = true
                    if (deletedPhoto && deletedPhotoQuiz) {
                        Toast.makeText(context, "Quiz data has been deleted.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to delete quiz data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
