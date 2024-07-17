package com.example.testfolder

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class PhotoActivity : AppCompatActivity() {

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    private lateinit var btnSelectImage: Button
    private lateinit var btnUploadImage: Button
    private lateinit var btnGoToList: Button
    private lateinit var selectedImageView: ImageView
    private lateinit var keywordEditText: EditText
    private lateinit var errorTextView: TextView
    private var imageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnUploadImage = findViewById(R.id.btnUploadImage)
        btnGoToList = findViewById(R.id.btnGoToList)
        selectedImageView = findViewById(R.id.selectedImageView)
        keywordEditText = findViewById(R.id.keywordEditText)
        errorTextView = findViewById(R.id.errorTextView)

        btnSelectImage.setOnClickListener {
            openGallery()
        }

        btnUploadImage.setOnClickListener {
            imageUri?.let { uri ->
                val exifData = extractExifData(uri)
                val keyword = keywordEditText.text.toString().trim()
                if (keyword.isEmpty()) {
                    errorTextView.text = "Please write keyword about your photo."
                    errorTextView.visibility = View.VISIBLE
                } else if (exifData.containsKey("GPSLatitude") && exifData.containsKey("GPSLongitude") && exifData.containsKey("DateTime")) {
                    uploadImageToFirebase(uri, exifData, keyword)
                } else {
                    errorTextView.text = "The selected image does not contain necessary location or time data."
                    errorTextView.visibility = View.VISIBLE
                }
            }
        }

        btnGoToList.setOnClickListener {
            val intent = Intent(this, ImageListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                val rotatedBitmap = imageUri?.let { rotateImageIfRequired(bitmap, it) }
                selectedImageView.setImageBitmap(rotatedBitmap)
                selectedImageView.visibility = View.VISIBLE
                btnUploadImage.visibility = View.VISIBLE
                errorTextView.visibility = View.GONE

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractExifData(imageUri: Uri): Map<String, String> {
        val exifData = mutableMapOf<String, String>()
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                val exifInterface = ExifInterface(inputStream)
                exifData["DateTime"] = exifInterface.getAttribute(ExifInterface.TAG_DATETIME).orEmpty()
                exifData["Flash"] = exifInterface.getAttribute(ExifInterface.TAG_FLASH).orEmpty()
                exifData["GPSLatitude"] = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE).orEmpty()
                exifData["GPSLongitude"] = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).orEmpty()
                exifData["ImageLength"] = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH).orEmpty()
                exifData["ImageWidth"] = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH).orEmpty()
                exifData["Make"] = exifInterface.getAttribute(ExifInterface.TAG_MAKE).orEmpty()
                exifData["Model"] = exifInterface.getAttribute(ExifInterface.TAG_MODEL).orEmpty()
                exifData["Orientation"] = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).orEmpty()
                inputStream.close()
            }
        } catch (e: IOException) {
            Log.e("EXIF", "Failed to read EXIF data", e)
        }
        return exifData
    }

    private fun rotateImageIfRequired(bitmap: Bitmap, imageUri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
        val exifInterface = ExifInterface(inputStream!!)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        inputStream.close()

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun uploadImageToFirebase(imageUri: Uri, exifData: Map<String, String>, keyword: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val storageRef = storageReference.child("images/$uid/${System.currentTimeMillis()}.jpg")

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask = storageRef.putBytes(data)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveExifDataToFirebase(uri.toString(), exifData, keyword)
                }
            }.addOnFailureListener { exception ->
                Log.e("Firebase", "Failed to upload image: ${exception.message}", exception)
                errorTextView.text = "Failed to upload image to Firebase: ${exception.message}"
                errorTextView.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExifDataToFirebase(imageUrl: String, exifData: Map<String, String>, keyword: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userImagesRef = database.child("users").child(uid).child("images").push()
            val exifDataWithKeyword = exifData.toMutableMap()
            exifDataWithKeyword["Keyword"] = keyword
            exifDataWithKeyword["ImageUrl"] = imageUrl

            userImagesRef.setValue(exifDataWithKeyword).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "EXIF data saved successfully")
                    errorTextView.text = "Photo metadata uploaded to the database."
                    errorTextView.setTextColor(getColor(R.color.black))
                    errorTextView.visibility = View.VISIBLE
                } else {
                    Log.e("Firebase", "Failed to save EXIF data", task.exception)
                    errorTextView.text = "Failed to save EXIF data to Firebase."
                    errorTextView.visibility = View.VISIBLE
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
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
