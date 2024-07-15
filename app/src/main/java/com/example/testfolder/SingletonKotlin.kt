package com.example.testfolder

import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.example.testfolder.utils.PreprocessTexts //진영이가 만들어준 매서드 가져다 쓰기 위해 추가
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import org.json.JSONObject

object SingletonKotlin {

    private var isInitialized = false
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    fun initialize(auth: FirebaseAuth, database: DatabaseReference) {
        // 초기화 메서드 - 싱글톤 객체를 초기화
        // FirebaseAuth 및 DatabaseReference 객체를 설정
        this.auth = auth
        this.database = database
        isInitialized = true
    }

    private fun checkInitialization() {
        // 초기화 확인 메서드 - 싱글톤 객체가 초기화되었는지 확인
        if (!isInitialized) {
            throw IllegalStateException("SingletonKotlin is not initialized, call initialize() method first.")
        }
    }

    fun loadUserCoins(coinText: TextView) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)

        userRef.child("coins").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val coins = snapshot.getValue(Long::class.java) ?: 0L
                coinText.text = coins.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(coinText.context, "Database ERROR", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateUserCoins(coinsToAdd: Long, coinText: TextView) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)

        userRef.child("coins").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentCoins = snapshot.getValue(Long::class.java) ?: 0L
                val newCoins = currentCoins + coinsToAdd
                userRef.child("coins").setValue(newCoins).addOnCompleteListener {
                    if (it.isSuccessful) {
                        coinText.text = newCoins.toString()
                    } else {
                        Toast.makeText(coinText.context, "Failed to update coins", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(coinText.context, "Database ERROR", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun loadUserBackground(frame: FrameLayout) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)

        userRef.child("selectedBackground").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val selectedBackground = snapshot.getValue(String::class.java) ?: "Default Room"
                val imageResource = getImageResourceByName(selectedBackground)
                frame.setBackgroundResource(imageResource)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(frame.context, "Database ERROR", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun saveUserBackground(itemName: String) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)
        userRef.child("selectedBackground").setValue(itemName)
    }

    fun loadPurchasedItems(buyItemList: MutableList<ShopItem>, adapter: ShopItemsAdapter) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userItemsRef = database.child("user_rooms").child(currentUser.uid)
        userItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (itemSnapshot in snapshot.children) {
                    val itemName = itemSnapshot.key
                    val itemPrice = itemSnapshot.child("price").getValue(Int::class.java) ?: 0
                    val purchased = itemSnapshot.child("purchased").getValue(Boolean::class.java) ?: false

                    if (purchased) {
                        val imageResource = getImageResourceByName(itemName)
                        val shopItem = ShopItem(imageResource, itemName!!, itemPrice)
                        buyItemList.add(shopItem)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리 냅둠
            }
        })
    }

    private fun getImageResourceByName(itemName: String?): Int {
        return when (itemName) {
            "Room 1" -> R.drawable.room01
            "Room 2" -> R.drawable.room02
            "Room 3" -> R.drawable.room03
            "Room 4" -> R.drawable.room04
            "Room 5" -> R.drawable.room05
            "Room 6" -> R.drawable.room06
            "Room 7" -> R.drawable.room07
            "Room 8" -> R.drawable.room08
            "Default Room" -> R.drawable.room3
            else -> R.drawable.room3 // 기본 이미지 설정
        }
    }

    fun getAuth(): FirebaseAuth {
        checkInitialization()
        return auth
    }

    fun getDatabase(): DatabaseReference {
        checkInitialization()
        return database
    }

    fun getCurrentUser(): FirebaseUser? {
        checkInitialization()
        return auth.currentUser
    }

    // ==========여기서 부터는 퀴즈 처리를 위한 용도==========

    // OX 퀴즈를 불러오는 매서드
    data class QuizItem(val question: String, val answer: String, val date: String)
    fun loadOXQuizData(callback: (List<QuizItem>) -> Unit) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val quizRef = database.child("ox_quiz").child(uid)

        quizRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quizList = mutableListOf<QuizItem>()
                for (dateSnapshot in snapshot.children) {   //이제 date만 비교하면 될거임.
                    val date = dateSnapshot.key ?: continue
                    for (quizSnapshot in dateSnapshot.children) {
                        val question = quizSnapshot.child("question").getValue(String::class.java) ?: ""
                        val answer = quizSnapshot.child("answer").getValue(String::class.java) ?: ""
                        quizList.add(QuizItem(question, answer, date)) // 퀴즈 작성 날짜 그냥 가져다 쓰면 됨
                    }
                }
                callback(quizList)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // 사지선다 - 객관식 퀴즈 불러오는 매서드
    data class MultipleChoiceQuizItem(val question: String, val choices: List<String>, val answer: String, val date: String)
    fun loadMultipleChoiceQuizData(callback: (List<MultipleChoiceQuizItem>) -> Unit) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val quizRef = database.child("mcq_quiz").child(uid)

        quizRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quizList = mutableListOf<MultipleChoiceQuizItem>()
                for (dateSnapshot in snapshot.children) {
                    val date = dateSnapshot.key ?: continue
                    for (quizSnapshot in dateSnapshot.children) {
                        val question = quizSnapshot.child("question").getValue(String::class.java) ?: ""
                        val optionsJson = quizSnapshot.child("choices").getValue(String::class.java) ?: ""
                        val options = PreprocessTexts.stringToStringArray(optionsJson)
                        if (options.size < 4) {
                            Log.e("Firebase", "Not enough options for question: $question")
                            continue
                        }
                        val answer = quizSnapshot.child("answer").getValue(String::class.java) ?: ""
                        // data 도 이제 " $year - $month-$day" 형식 불필요함.
                        quizList.add(MultipleChoiceQuizItem(question, options, answer, date)) //그냥 가져다 쓰면 됨
                    }
                }
                callback(quizList)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // 주관식 퀴즈 불러오는 매서드
    data class BlankQuizItem(val question: String, val answer: String, val date: String, val hint: String)
    fun loadBlankQuizData(callback: (List<BlankQuizItem>) -> Unit) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val quizRef = database.child("blank_quiz").child(uid)

        quizRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val quizList = mutableListOf<BlankQuizItem>()
                for (dateSnapshot in snapshot.children) {
                    val date = dateSnapshot.key ?: continue
                    for (quizSnapshot in dateSnapshot.children) {
                        val question = quizSnapshot.child("question").getValue(String::class.java) ?: ""
                        val answer = quizSnapshot.child("answer").getValue(String::class.java) ?: ""
                        val hint = quizSnapshot.child("hint").getValue(String::class.java) ?: ""
                        quizList.add(BlankQuizItem(question, answer, date, hint)) //그냥 가져다 쓰면 됨
                    }
                }
                callback(quizList)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // 게임 결과를 저장하는 메서드
    fun saveGameResult(gameType: String, correctAnswers: Int, totalTime: Long) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val resultRef = database.child("game_results").child(uid).push()

        val resultData = mapOf(
            "gameType" to gameType, // 게임 유형 추가 - OX, 4 Choice, Short Answer, miniNum, miniBaseball, miniSame별로 각각 지정해줄 수 있으려나?
            "correctAnswers" to correctAnswers,
            "totalTime" to totalTime,
            "timestamp" to System.currentTimeMillis()
        )

        resultRef.setValue(resultData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 데이터 저장 성공
            } else {
                // 데이터 저장 실패
            }
        }
    }

    //게임 결과를 불러오는 매서드를 만들어 놓자.
    fun loadGameResults(callback: (List<Map<String, Any>>) -> Unit) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val resultsRef = database.child("game_results").child(uid)

        resultsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val resultsList = mutableListOf<Map<String, Any>>()
                for (resultSnapshot in snapshot.children) {
                    val result = resultSnapshot.value as? Map<String, Any>
                    if (result != null) { //대응하는 값이 있다면 리스트에 저장함
                        resultsList.add(result)
                    }
                }
                callback(resultsList) //해당 리스트를 CALLBACK
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // 힌트를 불러오는 매서드
    fun loadHint(date: String, question: String, callback: (String) -> Unit) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val quizRef = database.child("blank_quiz").child(uid).child(date)

        quizRef.orderByChild("question").equalTo(question)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (quizSnapshot in snapshot.children) {
                        val hint = quizSnapshot.child("hint").getValue(String::class.java) ?: ""
                        callback(hint)
                        return
                    }
                    callback("No hint available.")
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Failed to load hint.")
                }
            })
    }
}
