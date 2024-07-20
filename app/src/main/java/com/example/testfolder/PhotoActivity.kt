package com.example.testfolder

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testfolder.utils.LoadingAnimation
import com.example.testfolder.utils.OpenAI
import com.example.testfolder.viewmodels.FirebaseViewModel
import com.example.testfolder.viewmodels.OpenAIViewModel
import com.example.testfolder.viewmodels.PhotoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

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
    private var imgName: String? = null
    private var imageUri: Uri? = null
    private var lastClickTime: Long = 0
    private val interval: Long = 1000

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    private lateinit var loadingAnimation: LoadingAnimation
    private lateinit var openAI: OpenAI
    private lateinit var openAIViewModel: ViewModel
    private lateinit var firebaseViewModel: ViewModel
    private lateinit var photoViewModel: ViewModel
    private lateinit var timeJson: JSONObject

    private var keyword: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private val originalFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
    private val targetFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.photo)

        val loadingBackgroundLayout = findViewById<ConstraintLayout>(R.id.loading_background_layout)
        val loadingImage = findViewById<ImageView>(R.id.loading_image)
        val loadingText = findViewById<TextView>(R.id.loading_text)
        val loadingTextDetail = findViewById<TextView>(R.id.loading_text_detail)
        val loadingTextDetail2 = findViewById<TextView>(R.id.loading_text_detail2)

        openAIViewModel   = ViewModelProvider(this).get(OpenAIViewModel::class.java)
        firebaseViewModel = ViewModelProvider(this).get(FirebaseViewModel::class.java)
        photoViewModel    = ViewModelProvider(this).get(PhotoViewModel::class.java)

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

        selectedImageView.clipToOutline = true

        loadingAnimation = LoadingAnimation(this,
            loadingBackgroundLayout, loadingImage, loadingText, loadingTextDetail, loadingTextDetail2, "Please wait")
        openAI = OpenAI(this, this,
            openAIViewModel as OpenAIViewModel,
            firebaseViewModel as FirebaseViewModel, loadingAnimation)
        openAI.fetchApiKey()

        btnSelectImage.setOnClickListener {
            openGallery()
        }

        btnUploadImage.setOnClickListener {
            // Prevent double click the button
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= interval) {
                lastClickTime = currentTime
                Log.d("UPLOAD_BUTTON", "Image upload button clicked.")
                imageUri?.let { uri ->
                    val exifData = extractExifData(uri)
                    val keyword = keywordEditText.text.toString().trim()
                    if (keyword.isEmpty()) {
                        errorTextView.text = "Please write keyword about your photo."
                        errorTextView.visibility = View.VISIBLE
                    } else if (exifData.containsKey("GPSLatitude") && exifData.containsKey("GPSLongitude") && exifData.containsKey(
                            "DateTime"
                        )
                    ) {
                        this.keyword = keyword
                        val timestamp = exifData["DateTime"]
                        val dateForDB = timestampToUKDate(timestamp!!).replace("/", " ")
                        val timeOfDay = timestampToTimeOfDay(timestamp)
                        val dayOfWeek = timestampToDayofweek(timestamp)
                        this.timeJson = JSONObject().apply {
                            put("timestamp", timestamp)
                            put("date", dateForDB)
                            put("dayofweek", dayOfWeek)
                            put("timeofday", timeOfDay)
                        }
                        loadingAnimation.showLoading()
                        uploadImageToFirebase(uri, exifData, keyword)
                    } else {
                        errorTextView.text =
                            "The selected image does not contain necessary location or time data."
                        errorTextView.visibility = View.VISIBLE
                    }
                }
            }
        }

        btnGoToList.setOnClickListener {
            val intent = Intent(this, ImageListActivity::class.java)
            startActivity(intent)
        }

        (photoViewModel as PhotoViewModel).urlLiveData.observe(this){ url ->
            Log.d("URL", "IMAGE URL 3: $url")
            // Observe the LiveData
            // Once the api key is fetched, generate new 3 types of quiz
            openAI.generateImageQuiz(
                url=url,
                keyword=keyword!!,
                timeJson=timeJson
            )
        }

        (openAIViewModel as OpenAIViewModel).imgQuizResponse.observe(this){ response ->
            coroutineScope.launch {
                (firebaseViewModel as FirebaseViewModel).save_img_OX_data(
                    dbReference = database,
                    auth = auth,
                    quiz = response,
                    timeJson = timeJson
                )
            }
        }

        (firebaseViewModel as FirebaseViewModel).imgQuizSaved.observe(this){
            Log.i("DB", "Data saved successfully")
            loadingAnimation.hideLoading()
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun timestampToTimeOfDay(timestamp: String): String {
        val date: Date? = originalFormat.parse(timestamp)
        val timeOfDay: String
        if (date != null) {
            when {
                // Morning
                ((date.hours >= 5) && (date.hours < 12)) -> timeOfDay = "Morning"
                // Afternoon
                ((date.hours >= 12) && (date.hours < 18)) -> timeOfDay = "Afternoon"
                // Evening
                ((date.hours >= 18) && (date.hours < 22)) -> timeOfDay = "Evening"
                // Night
                ((date.hours >= 22) && (date.hours < 24)) -> timeOfDay = "Night"
                ((date.hours >= 0) && (date.hours < 5)) -> timeOfDay = "Night"
                else -> throw Exception("Hour is not between 0-24")
            }
            return timeOfDay
        } else {
            throw Exception("Date is null")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timestampToDayofweek(timestamp: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
        val dateTime = LocalDateTime.parse(timestamp, formatter)
        val dayOfWeek = dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return dayOfWeek
    }

    private fun timestampToUKDate(timestamp: String): String {
        val date: Date? = originalFormat.parse(timestamp)
        val formattedDateString = date?.let { targetFormat.format(it) }
        return formattedDateString!!
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
            imgName = "${System.currentTimeMillis()}.jpg"
            Log.d("storage name ", imgName!!)

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask = storageRef.putBytes(data)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { url ->
//                    Log.d("URL", "IMAGE URL 1: $url")
//                    val preprocessedURL = url.toString().replace("\\", "")
//                    Log.d("URL", "IMAGE URL 2: $preprocessedURL")
//                    (photoViewModel as PhotoViewModel).setUrlLiveData(preprocessedURL)
                    saveExifDataToFirebase(url.toString(), exifData, keyword, imgName!!)
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

    private fun saveExifDataToFirebase(imageUrl: String, exifData: Map<String, String>, keyword: String, imgName: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userImagesRef = database.child("users").child(uid).child("images").push()
            val exifDataWithKeyword = exifData.toMutableMap()
            exifDataWithKeyword["Keyword"] = keyword
            exifDataWithKeyword["ImageUrl"] = imageUrl
            exifDataWithKeyword["imgName"] = imgName

            userImagesRef.setValue(exifDataWithKeyword).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loadingAnimation.hideLoading()
                    Log.d("Firebase", "EXIF data saved successfully")
                    errorTextView.text = "Photo metadata uploaded to the database."
                    errorTextView.setTextColor(getColor(R.color.black))
                    errorTextView.visibility = View.VISIBLE
                    // Create the arguments to the callable function.
                    val json = hashMapOf(
                        "prompt" to openAI.get_prompt_for_image_quiz(keyword, timeJson),
                        "url" to imageUrl,
                        "uid" to uid,
                        "date" to timeJson.getString("date"),
                    )
//                    val json = JSONObject().apply {
//                        put("prompt", openAI.get_prompt_for_image_quiz(keyword, timeJson))
//                        put("url", imageUrl)
//                        put("uid", uid)
//                        put("date", timeJson.getString("date"))
//                    }
                    val functions = FirebaseFunctions.getInstance()
                    Log.d("Firebase", "Called firebase function for gpt use")
                    functions
                        .getHttpsCallable("callChatGPTAndStoreResponseAboutImage")
                        .call(json)
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
