package com.example.testfolder.utils

import android.util.Log
import com.example.testfolder.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

class OpenAI {
    private val apiKey = BuildConfig.API_KEY
    val model = "gpt-3.5-turbo-0125"

    fun generate_OX_quiz_and_save(diary: String, numOfQuestions: Int, date: String) {
        val prompt = get_prompt_OX_quiz(diary, numOfQuestions)
        get_response_and_save(prompt, date)
    }

    private fun get_prompt_OX_quiz(diary: String, numOfQuestions: Int): String {
        val prompt_OX = "Generate $numOfQuestions O/X quiz based on the following diary." +
                "\nDiary: $diary" +
                "\nExample answer: {\"question\": \"I woke up at 9 AM\", \"answer\": \"O\"}" +
                "Each answer should be separated by \"\\n\", and don't add any introduction to your response."
        return prompt_OX
    }

    private fun get_response_and_save(prompt: String, date: String) {
        // Using coroutines to run this block in backgroud thread
        CoroutineScope(Dispatchers.IO).launch {
            val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
            val okHttpClient = OkHttpClient()
            val messages = JSONArray()
            val systemMsg = JSONObject()
            val userMsg = JSONObject()
            val json = JSONObject()
            // system message that is gonna be contained in the final json object
            systemMsg.put("role", "system")
            systemMsg.put("content", "You are a helpful assistant.")
            // user message that is gonna be contained in the final json object
            userMsg.put("role", "user")
            userMsg.put("content", prompt)
            // Create value of the key messages that is gonna be contained in the final json object
            messages.put(systemMsg)
            messages.put(userMsg)
            // Create a final json object to send through API call
            json.put("model", model)
            json.put("messages", messages)
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
                            val jsonObject = JSONObject(response.body?.string() ?: "")
                            val jsonArray = jsonObject.getJSONArray("choices")
                            // 아래 result 받아오는 경로가 좀 수정되었다.
                            val result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content").trim()
                            Log.i("GPT", "response: "+result.trim())
                            // Save the response to DB
                            save_OX_data(result, date)
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
    private fun save_OX_data(response: String, date: String) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val auth: FirebaseAuth = FirebaseAuth.getInstance() // FirebaseAuth 객체 초기화
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        if(uid != null) {
            var ref = database.reference.child("ox_quiz").child(uid).child(date)
            var dbTask = ref.removeValue()
            dbTask.addOnSuccessListener {
                Log.i("DB", "Data Deleted successfully.")
            }

            val quizList = response.split("\n")
            quizList.forEachIndexed { index, quiz ->
                ref = database.reference.child("ox_quiz").child(uid).child(date).child(index.toString())
                val jsonObject = JSONObject(quiz)
                val question = jsonObject.getString("question")
                val answer = jsonObject.getString("answer")
                val recordMap = mapOf(
                    "question" to question,
                    "answer" to answer
                )
                dbTask = ref.setValue(recordMap)
                dbTask.addOnSuccessListener {
                    Log.i("DB", "Data saved successfully")
                }
            }
        }
    }
}