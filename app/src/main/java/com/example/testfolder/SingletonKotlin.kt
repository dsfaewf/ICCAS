package com.example.testfolder

import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

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

    // 사용자 코인을 불러오는 메서드
    // 주어진 TextView에 사용자 코인 수를 표시
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

    // 사용자 배경 이미지를 불러오는 메서드
    // 주어진 FrameLayout에 사용자 배경 이미지를 설정
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

    // 사용자 배경 이미지를 저장하는 메서드
    // 사용자가 선택한 배경 이미지를 Firebase 데이터베이스에 저장
    fun saveUserBackground(itemName: String) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userRef = database.child("users").child(currentUser.uid)
        userRef.child("selectedBackground").setValue(itemName)
    }

    // 사용자가 구매한 아이템을 불러오는 메서드
    // 사용자에게 구매한 아이템을 MutableList에 추가하고, 어댑터를 통해 RecyclerView에 표시
    fun loadPurchasedItems(buyItemList: MutableList<ShopItem>, adapter: ShopItemsAdapter) {
        checkInitialization()
        val currentUser = auth.currentUser ?: return
        val userItemsRef = database.child("user_rooms").child(currentUser.uid)
        // 아니 기본 배경 왜 추가 안 되냐고!@#!#!@#
        // -> 밖에서 수동적으로 추가하는 방식으로 노선 변경
        // // //buyItemList.add(ShopItem(R.drawable.room3, "Default Room", 0)) // 이 녀석을 직접 페이지에 코드 추가하자
        userItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 기본 배경을 제외한 나머지 아이템들을 추가
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

    // 아이템 이름에 따라 이미지 리소스를 반환하는 함수
    // db에 저장된 아이템 이름에 해당하는 이미지를 반환
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

    fun getAuth(): FirebaseAuth { //파이어베이스 객체 반환
        checkInitialization()
        return auth
    }

    fun getDatabase(): DatabaseReference { //데이터베이스레퍼런스 객체 반환
        checkInitialization()
        return database
    }

    fun getCurrentUser(): FirebaseUser? { //현재 로그인된 유저 정보 객체 반환
        checkInitialization()
        return auth.currentUser
    }
}
