package com.example.testfolder

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class ShopActivity : AppCompatActivity(), ShopItemsAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopItemsAdapter
    private lateinit var shopItemList: List<ShopItem>
    private lateinit var coinText: TextView
    private lateinit var frame: FrameLayout
    private lateinit var buyBtn: TextView
    private lateinit var roomBtn: TextView
    private lateinit var decoBtn: TextView
    private lateinit var gameBtn: TextView
    private lateinit var buyLayout: LinearLayout
    private lateinit var clickedItem: ShopItem

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        auth = SingletonKotlin.getAuth()
        database = SingletonKotlin.getDatabase() //파이어베이스 객체 가져오기

        coinText = findViewById(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        val gifDrawable = catGif.drawable as GifDrawable
        frame = findViewById(R.id.shop_frame)
        buyBtn = findViewById(R.id.buy_button)
        buyLayout = findViewById(R.id.buy_layout)
        roomBtn = findViewById(R.id.room_btn)
        decoBtn = findViewById(R.id.deco_btn)
        gameBtn = findViewById(R.id.game_btn)
        gifDrawable.loopCount = 0 // 무한 반복

        recyclerView = findViewById(R.id.shop_items_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        shopItemList = listOf(
            ShopItem(R.drawable.room01, "Room 1", 10),
            ShopItem(R.drawable.room02, "Room 2", 30),
            ShopItem(R.drawable.room03, "Room 3", 50),
            ShopItem(R.drawable.room04, "Room 4", 70),
            ShopItem(R.drawable.room05, "Room 5", 90),
            ShopItem(R.drawable.room06, "Room 6", 100),
            ShopItem(R.drawable.room07, "Room 7", 150),
            ShopItem(R.drawable.room08, "Room 8", 200), //DB에 구매 가격을 불러올 수 있도록 INT형으로 가격 타입을 변경함 - 우석.
        )

        adapter = ShopItemsAdapter(shopItemList, this, this)
        recyclerView.adapter = adapter

        SingletonKotlin.loadUserCoins(coinText)

        // 현재 장착중인 배경으로 배경 설정
        SingletonKotlin.loadUserBackground(frame)

        buyBtn.setOnClickListener {
            checkItemAlreadyPurchased(clickedItem)
        }

        roomBtn.setOnClickListener {
            val intent = Intent(this, CatRoomActivity::class.java)
            startActivity(intent)
            finish()
        }
        decoBtn.setOnClickListener {
            val intent = Intent(this, DecoActivity::class.java)
            startActivity(intent)
            finish()
        }
        gameBtn.setOnClickListener {
            val intent = Intent(this, gamelistActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onItemClick(position: Int) {
        clickedItem = shopItemList[position]
        frame.setBackgroundResource(clickedItem.imageResource)
        buyBtn.visibility = View.VISIBLE
        buyLayout.visibility = View.VISIBLE
    }

    private fun checkItemAlreadyPurchased(clickedItem: ShopItem) { //구매했는지를 확인하는 함수
        val currentUser = auth.currentUser
        currentUser?.let {
            val userItemsRef = database.child("user_rooms").child(it.uid).child(clickedItem.name) //USER_ROOMS 태그에대해 비교
            userItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { //SNAPSHOT을 통해 DB에 존재하는지를 비교하여 확인
                    if (snapshot.exists()) {
                        // 이미 아이템을 구매한 경우 - DIALOG 표시
                        showAlreadyPurchasedDialog()
                    } else {
                        // 아이템을 구매하지 않은 경우 - 구매할건지 물음
                        showPurchaseConfirmationDialog(clickedItem)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // 에러 처리
                }
            })
        }
    }

    private fun showAlreadyPurchasedDialog() { //이미 구매한 건 경고메시지 알려주기
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Item Already Purchased")
            .setMessage("You have already purchased this item.")
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPurchaseConfirmationDialog(clickedItem: ShopItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Purchase Confirmation")
            .setMessage("Do you want to buy ${clickedItem.name} for ${clickedItem.price}?")
            .setPositiveButton("Buy") { dialog, which ->
                handlePurchase(clickedItem)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showNotEnoughCoinsDialog(clickedItem: ShopItem) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Not Enough Coins")
            .setMessage("You don't have enough coins to buy ${clickedItem.name}.")
            .setPositiveButton("OK") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handlePurchase(clickedItem: ShopItem) {
        val itemPrice = clickedItem.price  // 가격을 직접 사용
        val userCoins = coinText.text.toString().toInt()  // 사용자 보유 코인 가져오기

        if (userCoins < itemPrice) {
            showNotEnoughCoinsDialog(clickedItem) // 코인 부족할 때
        } else {
            // 여기에 아이템을 구매하는 코드를 작성합니다.
            // 예를 들어, 구매한 아이템을 데이터베이스에 업데이트하거나, 코인을 차감하는 등의 작업을 수행할 수 있습니다.

            // 구매 후 코인 차감 예시
            val remainingCoins = userCoins - itemPrice
            coinText.text = remainingCoins.toString()  // 화면에 남은 코인을 업데이트

            val currentUser = auth.currentUser //현재 유저 로그인 정보르르 불러오고
            currentUser?.let { //이상 없이 유저 존재한다면
                val userRef = database.child("users").child(it.uid) //UID 불러와서
                userRef.child("coins").setValue(remainingCoins) // DB에 코인 업데이트

                val userItemsRef = database.child("user_rooms").child(it.uid).child(clickedItem.name)
                val purchaseData = mapOf( //아이템을 구매했는지 여부와 가격을 저장하도록 함.
                    "purchased" to true,
                    "price" to itemPrice
                )
                userItemsRef.setValue(purchaseData) // 아이템 구매 정보 및 가격 저장

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Purchase Successful")
                    .setMessage("You bought ${clickedItem.name} for ${clickedItem.price} coins.")
                    .setPositiveButton("OK") { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

}
