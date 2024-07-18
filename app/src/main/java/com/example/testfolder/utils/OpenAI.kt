package com.example.testfolder.utils

import android.util.Log
import android.widget.EditText
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

fun main() {
    val json = JSONObject().apply {
//                    put("model", "gpt-4-turbo")
        put("model", "gpt-4o")
        put("temperature", 0)
    }
//    val json = JSONObject().apply {
//        put("test", "https://firebasestorage.googleapis.com/v0/b/iccas2024-main.appspot.com/o/images%2F94yToHXiXYc4jTFGFukjs4dNSLt1%2F1721225711882.jpg?alt=media&token=a874c05c-2983-4a7f-917c-63e9989f49bd")
//    }
    println(json.toString())
}

object OkHttpClientSingleton {
    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS) // Connection timeout
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // Read timeout
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // Write timeout
            .build()
    }
}

class OpenAI(
    lifecycleOwner: LifecycleOwner,
    context: AppCompatActivity,
    openAIViewModel: OpenAIViewModel,
    firebaseViewModel: FirebaseViewModel,
    loadingAnimation: LoadingAnimation,
    diaryEditText: EditText? = null) {
    companion object {
        const val PROB_TYPE_OX  = 0
        const val PROB_TYPE_MCQ = 1
        const val PROB_TYPE_BLANK  = 2
        const val PROB_TYPE_HINT  = 3
        const val PROB_TYPE_MCQ_REFINED = 4
        const val PROB_TYPE_IMAGE = 5
        const val NULL_STRING = "NULL"
    }
    private val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClientSingleton.instance
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var apiKey: String
    private lateinit var date: String
    private lateinit var response_gpt_OX: String
    private lateinit var response_gpt_MCQ: String
    private lateinit var response_gpt_blank: String
    private lateinit var response_gpt_hint: String
    private lateinit var response_gpt_image_OX: String
    private lateinit var diary: String
    private val diaryEditText: EditText?
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
        this.diaryEditText = diaryEditText
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

    fun generate_quiz_and_save(diary: String, numOfQuestions: Int) {
        val prompt_OX = get_prompt_OX_quiz(diary, numOfQuestions)
        val prompt_MCQ = get_prompt_MCQ_quiz(diary, numOfQuestions)
        val prompt_blank = get_prompt_blank_quiz(diary, numOfQuestions/2)
        this.diary = diary
        // Launch coroutines in parallel
        coroutineScope.launch {
            val resultOX = async { getResponse(prompt_OX, PROB_TYPE_OX) }
            val resultMCQ = async { getResponse(prompt_MCQ, PROB_TYPE_MCQ) }
            val resultBlank = async { getResponse(prompt_blank, PROB_TYPE_BLANK) }
            // Wait for all results
//            val results = awaitAll(resultOX, resultMCQ, resultBlank)
        }
    }

    fun generate_hint() {
        val prompt_hint = get_prompt_for_hint()
        coroutineScope.launch {
            getResponse(
                prompt_hint, PROB_TYPE_HINT,
                sysMsg = "You are Dictionary bot. Your job is to generate hints into multiple dictionaries without any bullets. Each dictionary must be separated by new line" +
                        "\nDictionary format: {\"hint\": \"~\"}"
            )
        }
    }

    fun generateImageQuiz(url: String, keyword: String, timeJson: JSONObject) {
        Log.d("URL", "IMAGE URL 4: $url")
        val prompt = get_prompt_for_image_quiz(keyword, timeJson)
        coroutineScope.launch {
            getResponse(prompt, PROB_TYPE_IMAGE, sysMsg = NULL_STRING, imageURL = url)
        }
    }

    fun get_prompt_for_image_quiz(keyword: String, timeJson: JSONObject): String {
        val prompt = "You're a quiz generator from users' journal." +
                "\nGenerate one O/X quiz based on the following info." +
                "\nKeyword: $keyword." +
                "\nTime of day: ${timeJson.getString("timeofday")}." +
                "\nDay of week: ${timeJson.getString("dayofweek")}." +
                "\nExample answer: {\"question\": \"I enjoyed my dinner\", \"answer\": \"O\"}" +
                "\nYou can also refer to this picture. Don't add any intro."
        return prompt
    }

    private fun get_prompt_OX_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions O/X quiz based on positive contents in the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: {\"question\": \"I woke up at 9 AM\", \"answer\": \"O\"}" +
                "\nThe number of O and X for answer should be balanced." +
                "\nUse \"I\" as the subject of the question."
//                "Each answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_prompt_MCQ_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions multiple choice quiz based on positive contents in the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: " +
                "{\"question\": \"What did you have for lunch?\", " +
                "\"choices\": [\"pasta\", \"pizza\", \"sandwich\", \"burger\"], " +
                "\"answer\": \"pizza\"}" +
                "\nUse \"I\" as the subject of the question."
//                "\nEach answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_prompt_blank_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions blank quiz based on positive contents in the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: " +
                "{\"question\": \"I had <blank> for lunch.\", " +
                "\"answer\": \"pizza\"}" +
                "\nEach question must be short and have only one <blank>." +
                "\nEach <blank> must match just one word." +
                "\nUse \"I\" as the subject of the question."
//                "\nEach answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_prompt_for_hint(): String {
        val prompt_hint =
            "Hints should be generated based on the given diary." +
            "\nDiary: ${this.diary}" +
            "\nGenerate hint for each question by referring to the following example." +
            "\nExample: {\"question\": \"I had <blank> for lunch\", \"answer\": \"pizza\", \"hint\": \"I went to an Italian restaurant\"}" +
            "\nThe hint must not contain the problem's answer." +
            "\nTarget questions: ${this.response_gpt_blank}"
//                    "\nEach answer should be separated by \"\\n\""
//        Log.d("PROMPT_HINT", prompt_hint)
//        val prompt_hint =
//            "You're a bot generating hints for answers that are filled in blanks. Hint should be generated based on the given diary." +
//                    "\nDiary: ${this.diary}" +
//                    "\nGenerate hint for each question by referring to the following example." +
//                    "\nExample: {\"question\": \"I had <blank> for lunch\", \"answer\": \"pizza\", \"hint\": \"I went to an Italian restaurant\"}" +
//                    "\nThe hint must not contain the problem's answer." +
//                    "\nTarget questions: ${this.response_gpt_blank}"
        return prompt_hint
    }

    private fun get_prompt_for_refining_response(generatedQuiz: String): String {
        val prompt =
            "Your job is removing fake information from the input." +
            "Input: $generatedQuiz" +
            "\nThe input consists of multiple questions." +
            "\nGet rid of any question having fake info based on the given diary from the original questions." +
            "\nDiary: ${this.diary}"
//            "\nEach answer should be separated by \"\\n\""
        return prompt
    }

    private fun getRequestBody(prompt: String, probType: Int, sysMsg: String, imageURL: String): RequestBody {
        when (probType) {
            PROB_TYPE_IMAGE -> {
                Log.d("URL", "IMAGE URL 5: $imageURL")
                val messages = JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", JSONArray().apply {
                                put(
                                    JSONObject().apply {
                                        put("type", "text")
                                        put("text", prompt)
                                    }
                                )
                                put(
                                    JSONObject().apply {
                                        put("type", "image_url")
                                        put("image_url", JSONObject().apply {
                                            put("url", imageURL)
                                        })
                                    }
                                )
                            })
                        }
                    )
                }
                val json = JSONObject().apply {
//                    put("model", "gpt-4-turbo")
                    put("model", "gpt-4o")
                    put("messages", messages)
//                    put("temperature", 0)
                }
                Log.d("GPT", "Request body: $json")
                return json.toString().toRequestBody(mediaType)
            }
            else -> {
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", if (sysMsg == NULL_STRING) "You are Dictionary bot. Your job is to generate quiz into multiple dictionaries without any bullets. Dictionaries must be separated by a new line. There must not be any new lines in each dictionary." else sysMsg)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }

                val json = JSONObject().apply {
                    put("model", model)
                    put("messages", messages)
                    put("temperature", 0)
                }
                Log.d("GPT", "Request body: $json")
                return json.toString().toRequestBody(mediaType)
            }
        }
    }

    private suspend fun getResponse(prompt: String, probType: Int, sysMsg: String = NULL_STRING, imageURL: String = NULL_STRING): String {
        val requestBody = getRequestBody(prompt, probType, sysMsg, imageURL)
//        Log.d("GPT", "Request body: $requestBody")
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonObject = JSONObject(responseBody)
                        val result = jsonObject.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()

                        processResponse(result, probType)
                        result
                    } ?: throw IOException("Empty response body")
                } else {
                    throw IOException("Failed to load response: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("GPT", "Error fetching response", e)
                throw e
            }
        }
    }

    private fun processResponse(result: String, probType: Int) {
        when (probType) {
            PROB_TYPE_OX -> {
                Log.i("GPT-RECEIVED", "Got OX response")
                response_gpt_OX = result
                delete_data("ox_quiz", PROB_TYPE_OX)
            }
            PROB_TYPE_MCQ -> {
                Log.i("GPT-RECEIVED", "Got MCQ response")
                val promptForRefiningResponse = get_prompt_for_refining_response(result)
                coroutineScope.launch { getResponse(promptForRefiningResponse, PROB_TYPE_MCQ_REFINED) }
            }
            PROB_TYPE_MCQ_REFINED -> {
                Log.i("GPT-RECEIVED", "Got refined MCQ response")
                response_gpt_MCQ = result
                delete_data("mcq_quiz", PROB_TYPE_MCQ)
            }
            PROB_TYPE_BLANK -> {
                Log.i("GPT-RECEIVED", "Got BLANK response")
                response_gpt_blank = result
                openAIViewModel.setGotResponseForBlankLiveData(true)
            }
            PROB_TYPE_HINT -> {
                Log.i("GPT-RECEIVED", "Got HINT response")
                response_gpt_hint = result
                delete_data("blank_quiz", PROB_TYPE_BLANK)
            }
            PROB_TYPE_IMAGE -> {
                Log.i("GPT-RECEIVED", "Got IMAGE quiz response")
                openAIViewModel.setImgQuizResponse(result)
            }
            else -> throw Exception("Wrong problem type specified")
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
            val quizMap = mutableMapOf<String, Map<String, String>>()
            var quizList: List<String>
            // Preprocess the response from GPT
            quizList = if(this.response_gpt_OX.contains("} {")) {
                getStringListByCleaningSpaceBetweenDict(this.response_gpt_OX)
            } else {
                this.response_gpt_OX.split("\n")
            }
            // Filter out strings that consist only of white spaces
            quizList = quizList.filter { it.isNotBlank() }
            quizList.forEachIndexed { index, quiz ->
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val answer = jsonObject.getString("answer")
                val recordMap = mapOf(
                    "question" to question,
                    "answer" to answer
                )
                quizMap[index.toString()] = recordMap
            }
            val ref = database.reference.child("ox_quiz").child(uid).child(this.date)
            val dbTask = ref.setValue(quizMap)
            dbTask.addOnSuccessListener {
                this.savedOX = true
                if(savedOX && savedMCQ && savedBlank) {
                    loadingAnimation.hideLoading()
                    Log.i("DB", "Data saved successfully")
                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    this.savedOX = false
                    this.savedMCQ = false
                    this.savedBlank = false
                    firebaseViewModel.setAllQuizSaved(true)
                }
            }
        }
    }

    fun save_MCQ_data() {
        if (uid != null) {
            val responseGptMCQ = this.response_gpt_MCQ.trimIndent()
            Log.i("GPT-MCQ-REFINED", "response: " + responseGptMCQ)
            val quizMap = mutableMapOf<String, Map<String, String>>()
            // Preprocess the response from GPT
            var quizList: List<String> = if (responseGptMCQ.contains("} {")) {
                getStringListByCleaningSpaceBetweenDict(responseGptMCQ)
            } else {
                responseGptMCQ.split("\n").map { it.trim() }
            }

            // Filter out strings that consist only of white spaces
            quizList = quizList.filter { it.isNotBlank() }
            quizList.forEachIndexed { index, quiz ->
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val choices = jsonObject.getString("choices")
                val answer = jsonObject.getString("answer")
                val recordMap = mapOf(
                    "question" to question,
                    "choices" to choices,
                    "answer" to answer
                )
                quizMap[index.toString()] = recordMap
            }
            val ref = database.reference.child("mcq_quiz").child(uid).child(this.date)
            val dbTask = ref.setValue(quizMap)
            dbTask.addOnSuccessListener {
                this.savedMCQ = true
                if(savedOX && savedMCQ && savedBlank) {
                    loadingAnimation.hideLoading()
                    Log.i("DB", "Data saved successfully")
                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                    this.savedOX = false
                    this.savedMCQ = false
                    this.savedBlank = false
                    firebaseViewModel.setAllQuizSaved(true)
                }
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun save_blank_quiz_data() {
        if(uid != null) {
            Log.i("GPT-BLANK", "response: " + this.response_gpt_blank)
            Log.i("GPT-HINT", "response: " + this.response_gpt_hint)
            var quizListBlank: List<String>
            var quizListHint: List<String>
            // Prepare a map to hold all the quiz records
            val quizMap = mutableMapOf<String, Map<String, String>>()

            // Preprocess the response from GPT
            quizListBlank = if(this.response_gpt_blank.contains("} {")) {
                getStringListByCleaningSpaceBetweenDict(this.response_gpt_blank)
            } else {
                this.response_gpt_blank.split("\n")
            }
            quizListHint = if(this.response_gpt_hint.contains("} {")) {
                getStringListByCleaningSpaceBetweenDict(this.response_gpt_hint)
            } else {
                this.response_gpt_hint.split("\n")
            }
            // Filter out strings that consist only of white spaces
            quizListBlank = quizListBlank.filter { it.isNotBlank() }
            quizListHint = quizListHint.filter { it.isNotBlank() }
//            val quizCountDiff = abs(quizListBlank.count() - quizListHint.count())
//            if((quizCountDiff >= 2) || (quizListHint.count() > quizListBlank.count())) {
            if(quizListHint.count() > quizListBlank.count()) {
                    diaryEditText!!.setText(diary)
                loadingAnimation.hideLoading()
                Log.i("ERROR", "quizListBlank.count()(${quizListBlank.count()}) != quizListHint.count()(${quizListHint.count()})")
                Log.i("ERROR", "quizListBlank: $quizListBlank")
                Log.i("ERROR", "quizListHint: $quizListHint")
                Toast.makeText(this.context, "Please click on the save button again", Toast.LENGTH_SHORT).show()
            }
            else {
                quizListHint.forEachIndexed { index, quiz ->
                    val jsonObjectBlank = JSONObject(quizListBlank.get(index))
                    val question = jsonObjectBlank.getString("question")
                    val answer = jsonObjectBlank.getString("answer")
                    val jsonObjectHint = JSONObject(quiz)
                    val hint = jsonObjectHint.getString("hint")
                    val recordMap = mapOf(
                        "question" to question,
                        "answer" to answer,
                        "hint" to hint
                    )
                    quizMap[index.toString()] = recordMap
                }
//                val ref = database.reference.child("blank_quiz").child(uid).child(this.date)
//                    .child(index.toString())
                val ref = database.reference.child("blank_quiz").child(uid).child(this.date)
                val dbTask = ref.setValue(quizMap)
                dbTask.addOnSuccessListener {
                    this.savedBlank = true
                    if (savedOX && savedMCQ && savedBlank) {
                        loadingAnimation.hideLoading()
                        Log.i("DB", "Data saved successfully")
                        Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                        this.savedOX = false
                        this.savedMCQ = false
                        this.savedBlank = false
                        firebaseViewModel.setAllQuizSaved(true)
                    }
//                    Toast.makeText(this.context, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getStringListByCleaningSpaceBetweenDict(response: String): List<String> {
        // Split the response string by spaces between JSON objects
        val jsonStrings = response.split("} {")

        // Initialize a list to store the JSON objects
        val jsonObjectList = mutableListOf<String>()

        // Process each split string to create complete JSON strings
        for (jsonString in jsonStrings) {
            // Clean up the JSON strings by adding missing braces
            val cleanedJsonString = when {
                jsonString.startsWith("{") && jsonString.endsWith("}") -> jsonString
                jsonString.startsWith("{") -> "$jsonString}"
                jsonString.endsWith("}") -> "{$jsonString"
                else -> "{$jsonString}"
            }
            // Parse the JSON string and add it to the list
            jsonObjectList.add(cleanedJsonString)
        }
        return jsonObjectList
    }
}