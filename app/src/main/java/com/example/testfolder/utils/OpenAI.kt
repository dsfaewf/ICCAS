package com.example.testfolder.utils

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.example.testfolder.viewmodels.OpenAIViewModel
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

class OpenAI(lifecycleOwner: LifecycleOwner,
             context: AppCompatActivity,
             openAIViewModel: OpenAIViewModel,
             firebaseViewModel: FirebaseViewModel,
             loadingAnimation: LoadingAnimation) {
    companion object {
        const val PROB_TYPE_OX  = 0
        const val PROB_TYPE_MCQ = 1
        const val PROB_TYPE_BLANK  = 2
        const val PROB_TYPE_HINT  = 3
        const val NULL_STRING = "NULL"
    }
    private lateinit var apiKey: String
    private lateinit var date: String
    private lateinit var response_gpt_OX: String
    private lateinit var response_gpt_MCQ: String
    private lateinit var response_gpt_blank: String
    private lateinit var response_gpt_hint: String
    private lateinit var diary: String
    private var lifecycleOwner: LifecycleOwner
    private var context: AppCompatActivity
    private var openAIViewModel: OpenAIViewModel
    private var firebaseViewModel: FirebaseViewModel
    private var loadingAnimation: LoadingAnimation
    val model = "gpt-3.5-turbo-0125"
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화
    val currentUser = auth.currentUser
    val uid = currentUser?.uid
    var savedOX    = false
    var savedMCQ   = false
    var savedBlank = false

    init {
        this.lifecycleOwner = lifecycleOwner
        this.context = context
        this.openAIViewModel = openAIViewModel
        this.firebaseViewModel = firebaseViewModel
        this.loadingAnimation = loadingAnimation
    }

    fun updateDate(date: String) {
        this.date = date.replace("/", " ")
    }

    fun fetchApiKey() {
        if (this::apiKey.isInitialized && apiKey.isNotEmpty()) {
            openAIViewModel.setApiKey(this.apiKey)
        } else {
            getOpenaiApiKey().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    this.apiKey = task.result
                    openAIViewModel.setApiKey(this.apiKey)
                    // Use the API key
                    Log.d("API_KEY", "Received API Key successfully")
                } else {
                    Log.e("API_KEY_ERROR", "Error fetching API key", task.exception)
                }
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

    fun generate_OX_quiz_and_save(diary: String, numOfQuestions: Int) {
        val prompt_OX = get_prompt_OX_quiz(diary, numOfQuestions)
        val prompt_MCQ = get_prompt_MCQ_quiz(diary, numOfQuestions)
        val prompt_blank = get_prompt_blank_quiz(diary, numOfQuestions/2)
        this.diary = diary
        get_response(prompt_OX, PROB_TYPE_OX)
        get_response(prompt_MCQ, PROB_TYPE_MCQ)
        get_response(prompt_blank, PROB_TYPE_BLANK)
    }

    fun generate_hint() {
        val prompt_hint = get_prompt_for_hint()
        get_response(prompt_hint, PROB_TYPE_HINT,
            sysMsg = "You are Dictionary bot. Your job is to generate hints into multiple dictionaries without any bullets" +
                    "\nDictionary format: {\"hint\": \"~\"}"
        )
//        get_response_and_save(prompt_hint, PROB_TYPE_HINT)
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
                "\nEach question must have only one <blank> that has to match one word." +
                "\nEach question must be one sentence." +
                "\nEach answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_prompt_for_hint(): String {
//        val prompt_hint = "Generate a hint for each question by referring to the following example." +
//                "\nExample: {\"question\": \"I had <blank> for lunch\", \"answer\": \"pizza\", \"hint\": \"It is an Italian food.\"}" +
//                "\nTarget Question: ${this.response_gpt_blank}" +
//                "\nAnswer format: {\"hint\": \"\"}" +
//                "\nEach answer should be separated by \"\\n\""
        val prompt_hint =
                "You're a bot generating hint for answers that should be filled in the blank. Hint should be generated based on the given diary." +
                "\nDiary: ${this.diary}" +
                "\nGenerate hint for each question by referring to the following example." +
                "\nExample: {\"question\": \"I had <blank> for lunch\", \"answer\": \"pizza\", \"hint\": \"I went to an Italian restaurant\"}" +
                "\nTarget questions: ${this.response_gpt_blank}" +
                "\nEach answer should be separated by \"\\n\""
        Log.d("PROMPT_HINT", prompt_hint)
        return prompt_hint
    }

    private fun get_response(prompt: String, probType: Int, sysMsg: String = NULL_STRING) {
        // Using coroutines to run this block in background thread
        CoroutineScope(Dispatchers.IO).launch {
            val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
            val okHttpClient = OkHttpClient()
            val messages = JSONArray()
            val systemMsg = JSONObject()
            val userMsg = JSONObject()
            val json = JSONObject()
            // system message that is gonna be contained in the final json object
            systemMsg.put("role", "system")
            if(sysMsg == NULL_STRING) {
                systemMsg.put("content", "You are Dictionary bot. Your job is to generate quiz into multiple dictionaries without any bullets")
            } else {
                systemMsg.put("content", sysMsg)
            }
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
                                    openAIViewModel.setGotResponseForBlankLiveData(true)
                                    // Delete existing diary for the day first
//                                    delete_data("blank_quiz", PROB_TYPE_BLANK)
                                }

                                PROB_TYPE_HINT -> {
                                    // update response
                                    response_gpt_hint = result
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
                    PROB_TYPE_HINT -> firebaseViewModel.set_hint_table_deleted(true)
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
                val ref = database.reference.child("ox_quiz").child(uid).child(this.date)
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
                    this.savedOX = true
                    if(savedOX && savedMCQ && savedBlank) {
                        loadingAnimation.hideLoading()
                        Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        this.savedOX = false
                        this.savedMCQ = false
                        this.savedBlank = false
                        firebaseViewModel.setAllQuizSaved(true)
                    }
                    Log.i("DB", "Data saved successfully")
                }
            }
        }
    }

    fun save_MCQ_data() {
        if (uid != null) {
            Log.i("GPT-MCQ", "response: " + this.response_gpt_MCQ)
            val quizList = this.response_gpt_MCQ.split("\n")
            quizList.forEachIndexed { index, quiz ->
                val ref = database.reference.child("mcq_quiz").child(uid).child(this.date)
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
                    this.savedMCQ = true
                    if(savedOX && savedMCQ && savedBlank) {
                        loadingAnimation.hideLoading()
                        Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        this.savedOX = false
                        this.savedMCQ = false
                        this.savedBlank = false
                        firebaseViewModel.setAllQuizSaved(true)
                    }
                    Log.i("DB", "Data saved successfully")
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun save_blank_quiz_data() {
        if(uid != null) {
            Log.i("GPT-BLANK", "response: " + this.response_gpt_hint)
            val quizList = this.response_gpt_hint.split("\n")
            quizList.forEachIndexed { index, quiz ->
                val ref = database.reference.child("blank_quiz").child(uid).child(this.date)
                    .child(index.toString())
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val answer = jsonObject.getString("answer")
                val hint = jsonObject.getString("hint")
                val recordMap = mapOf(
                    "question" to question,
                    "answer" to answer,
                    "hint" to hint
                )
                val dbTask = ref.setValue(recordMap)
                dbTask.addOnSuccessListener {
                    this.savedBlank = true
                    if(savedOX && savedMCQ && savedBlank) {
                        loadingAnimation.hideLoading()
                        Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        this.savedOX = false
                        this.savedMCQ = false
                        this.savedBlank = false
                        firebaseViewModel.setAllQuizSaved(true)
                    }
                    Log.i("DB", "Data saved successfully")
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun save_hint_data() {
        if(uid != null) {
            Log.i("GPT-HINT", "response: " + this.response_gpt_hint)
            val quizList = this.response_gpt_hint.split("\n")
            quizList.forEachIndexed { index, quiz ->
                val ref = database.reference.child("hint_for_blank").child(uid).child(this.date)
                    .child(index.toString())
                val jsonObject = JSONObject(quiz)
                val hint = jsonObject.getString("hint")
                val recordMap = mapOf(
                    "hint" to hint,
                )
                val dbTask = ref.setValue(recordMap)
                dbTask.addOnSuccessListener {
                    Log.i("DB", "Data saved successfully")
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}