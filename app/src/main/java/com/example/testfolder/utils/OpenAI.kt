package com.example.testfolder.utils

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.example.testfolder.viewmodels.ApiKeyViewModel
import com.example.testfolder.viewmodels.FirebaseViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class OpenAI(lifecycleOwner: LifecycleOwner, apiKeyViewModel: ApiKeyViewModel, firebaseViewModel: FirebaseViewModel) {
    companion object {
        const val PROB_TYPE_OX  = 0
        const val PROB_TYPE_MCQ = 1
        const val PROB_TYPE_BLANK  = 2
    }
    private lateinit var apiKey: String
    private lateinit var loadingAnimation: LoadingAnimation
    private lateinit var date: String
    private lateinit var response_gpt_OX: String
    private lateinit var response_gpt_MCQ: String
    private lateinit var response_gpt_blank: String
    private var lifecycleOwner: LifecycleOwner
    private var apiKeyViewModel: ApiKeyViewModel
    private var firebaseViewModel: FirebaseViewModel
    val model = "gpt-3.5-turbo-0125"
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화
    val currentUser = auth.currentUser
    val uid = currentUser?.uid

    init {
        this.lifecycleOwner = lifecycleOwner
        this.apiKeyViewModel = apiKeyViewModel
        this.firebaseViewModel = firebaseViewModel
    }

    fun updateDate(date: String) {
        this.date = date
    }

    fun fetchApiKey() {
        getOpenaiApiKey().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                this.apiKey = task.result
                apiKeyViewModel.setApiKey(this.apiKey)
                // Use the API key
                Log.d("API_KEY", "Received API Key successfully")
            } else {
                Log.e("API_KEY_ERROR", "Error fetching API key", task.exception)
            }
        }
    }

    private fun getOpenaiApiKey(): Task<String> {
        val functions = FirebaseFunctions.getInstance()

        return functions
            .getHttpsCallable("getOpenaiApiKey")
            .call()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Unknown error occurred")
                }

                val resultData = task.result?.data as? Map<*, *>
                    ?: throw Exception("Invalid response format")
                val apiKey = resultData["apiKey"] as? String
                    ?: throw Exception("API key not found in response")
//                Log.d("API_KEY", apiKey)
                apiKey
            }
    }

    fun generate_OX_quiz_and_save(loadingAnimation: LoadingAnimation, diary: String, numOfQuestions: Int) {
        this.loadingAnimation = loadingAnimation
        val prompt_OX = get_prompt_OX_quiz(diary, numOfQuestions)
        val prompt_MCQ = get_prompt_MCQ_quiz(diary, numOfQuestions)
        val prompt_blank = get_prompt_blank_quiz(diary, numOfQuestions/2)
        get_response_and_save(prompt_OX, PROB_TYPE_OX)
        get_response_and_save(prompt_MCQ, PROB_TYPE_MCQ)
        get_response_and_save(prompt_blank, PROB_TYPE_BLANK)
    }

    private fun get_prompt_OX_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions O/X quiz based on the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: {\"question\": \"I woke up at 9 AM\", \"answer\": \"O\"}" +
                "Each answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_prompt_MCQ_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions multiple choice quiz based on the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: " +
                "{\"question\": \"What did you have for lunch?\", " +
                "\"choices\": [\"pasta\", \"pizza\", \"sandwich\", \"burger\"], " +
                "\"answer\": \"pizza\"}" +
                "Each answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_prompt_blank_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions blank quiz based on the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: " +
                "{\"question\": \"I had <blank> for lunch.\", " +
                "\"answer\": \"pizza\"}" +
                "Don't generate time related quiz. Each question must have only one <blank> which has to match one word." +
                "Each answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_response_and_save(prompt: String, probType: Int) {
        // Using coroutines to run this block in backgroud thread
        CoroutineScope(Dispatchers.Main).launch {
            val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
            val okHttpClient = OkHttpClient()
            val messages = JSONArray()
            val systemMsg = JSONObject()
            val userMsg = JSONObject()
            val json = JSONObject()
            // system message that is gonna be contained in the final json object
            systemMsg.put("role", "system")
            systemMsg.put("content", "You are Dictionary bot. Your job is to generate quiz into multiple dictionaries without any bullets")
            // user message that is gonna be contained in the final json object
            userMsg.put("role", "user")
            userMsg.put("content", prompt)
            // Create value of the key messages that is gonna be contained in the final json object
            messages.put(systemMsg)
            messages.put(userMsg)
            // Create a final json object to send through API call
            json.put("model", model)
            json.put("messages", messages)
            json.put("temperature", 0)
            val requestBody: RequestBody = json.toString().toRequestBody(mediaType)
            val request: Request =
                Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            Log.d("GPT-CALL", "Response received.")
                            val jsonObject = JSONObject(response.body?.string() ?: "")
                            val jsonArray = jsonObject.getJSONArray("choices")
                            // 아래 result 받아오는 경로가 좀 수정되었다.
                            val result = jsonArray.getJSONObject(0).getJSONObject("message")
                                .getString("content").trim()
                            // Save the response to DB
                            when (probType) {
                                PROB_TYPE_OX -> {
                                    // update response
                                    response_gpt_OX = result
                                    // Delete existing diary for the day first
                                    delete_data("ox_quiz", PROB_TYPE_OX)
                                }

                                PROB_TYPE_MCQ -> {
                                    // update response
                                    response_gpt_MCQ = result
                                    // Delete existing diary for the day first
                                    delete_data("mcq_quiz", PROB_TYPE_MCQ)
                                }

                                PROB_TYPE_BLANK -> {
                                    // update response
                                    response_gpt_blank = result
                                    // Delete existing diary for the day first
                                    delete_data("blank_quiz", PROB_TYPE_BLANK)
                                }

                                else -> {
                                    throw Exception("Wrong problem type specified")
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        Log.i("GPT", "Failed to load response due to ${response.body?.string()}")
                    }
                }

                override fun onFailure(call: Call, e: java.io.IOException) {
                    Log.i("GPT", "onFailure: ")
                }
            })
        }
    }

    private fun delete_data(tableName: String, probType: Int) {
        if (uid != null) {
            val ref = database.reference.child(tableName).child(uid).child(this.date)
            val dbTask = ref.removeValue()
            dbTask.addOnSuccessListener {
                Log.i("DB", "Data Deleted successfully.")
                when (probType) {
                    PROB_TYPE_OX -> firebaseViewModel.set_OX_table_deleted(true)
                    PROB_TYPE_MCQ -> firebaseViewModel.set_MCQ_table_deleted(true)
                    PROB_TYPE_BLANK -> firebaseViewModel.set_blank_table_deleted(true)
                    else -> throw Exception("Got a wrong problem type to trigger the LiveData")
                }

            }
        } else {
            Log.i("DB", "UID not found.")
        }
    }

    fun save_OX_data() {
        if (uid != null) {
            Log.i("GPT-OX", "response: " + this.response_gpt_OX)
            val quizList = this.response_gpt_OX.split("\n")
            quizList.forEachIndexed { index, quiz ->
                val ref = database.reference.child("ox_quiz").child(uid).child(date)
                    .child(index.toString())
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val answer = jsonObject.getString("answer")
                val recordMap = mapOf(
                    "question" to question,
                    "answer" to answer
                )
                val dbTask = ref.setValue(recordMap)
                dbTask.addOnSuccessListener {
//                    this.loadingAnimation.hideLoading()
                    Log.i("DB", "Data saved successfully")
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun save_MCQ_data() {
        if (uid != null) {
            Log.i("GPT-MCQ", "response: " + this.response_gpt_MCQ)
            val quizList = this.response_gpt_MCQ.split("\n")
            quizList.forEachIndexed { index, quiz ->
                val ref = database.reference.child("mcq_quiz").child(uid).child(date)
                    .child(index.toString())
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val choices = jsonObject.getString("choices")
                val answer = jsonObject.getString("answer")
                val recordMap = mapOf(
                    "question" to question,
                    "choices" to choices,
                    "answer" to answer
                )
                val dbTask = ref.setValue(recordMap)
                dbTask.addOnSuccessListener {
//                    this.loadingAnimation.hideLoading()
                    Log.i("DB", "Data saved successfully")
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun save_blank_quiz_data() {
        if(uid != null) {
            Log.i("GPT-BLANK", "response: " + this.response_gpt_blank)
            val quizList = this.response_gpt_blank.split("\n")
            quizList.forEachIndexed { index, quiz ->
                val ref = database.reference.child("blank_quiz").child(uid).child(date)
                    .child(index.toString())
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val answer = jsonObject.getString("answer")
                val recordMap = mapOf(
                    "question" to question,
                    "answer" to answer
                )
                val dbTask = ref.setValue(recordMap)
                dbTask.addOnSuccessListener {
//                    this.loadingAnimation.hideLoading()
                    Log.i("DB", "Data saved successfully")
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}