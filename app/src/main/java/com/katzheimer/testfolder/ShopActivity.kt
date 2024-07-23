package com.katzheimer.testfolder

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        Log.d("ShopActivity", "onCreate: Started")

        auth = SingletonKotlin.getAuth()
        database = SingletonKotlin.getDatabase() // 파이어베이스 객체 가져오기

        coinText = findViewById(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        val newCatGif = findViewById<GifImageView>(R.id.newcat_gif)

        // 루프 설정
        (catGif.drawable as? GifDrawable)?.apply {
            loopCount = 0 // 무한 반복
            start()
        }
        (newCatGif.drawable as? GifDrawable)?.apply {
            loopCount = 0 // 무한 반복
            start()
        }

        frame = findViewById(R.id.shop_frame)
        buyBtn = findViewById(R.id.buy_button)
        buyLayout = findViewById(R.id.buy_layout)
        roomBtn = findViewById(R.id.room_btn)
        decoBtn = findViewById(R.id.deco_btn)
        gameBtn = findViewById(R.id.game_btn)

        recyclerView = findViewById(R.id.shop_items_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        shopItemList = listOf(
            ShopItem(R.drawable.room01, "Room 1", 150),
            ShopItem(R.drawable.room02, "Room 2", 200),
            ShopItem(R.drawable.room03, "Room 3", 250),
            ShopItem(R.drawable.room04, "Room 4", 300),
            ShopItem(R.drawable.room05, "Room 5", 350),
            ShopItem(R.drawable.room06, "Room 6", 400),
            ShopItem(R.drawable.room07, "Room 7", 450),
            ShopItem(R.drawable.room08, "Room 8", 450),
            ShopItem(R.drawable.colosseum, "Catlosseum", 1000),
            ShopItem(R.drawable.statueofliberty, "Cat of liberty", 1000),
            ShopItem(R.drawable.effel, "Effel cat", 1000),
            ShopItem(R.drawable.greatwall, "Great wall of Cat", 1000),
            ShopItem(R.drawable.operahouse, "Catperahouse", 1000),
            ShopItem(R.drawable.cat_friend1, "Cat Friend 1", 1000) // 추가된 고양이 친구
        )

        adapter = ShopItemsAdapter(shopItemList, this, this)
        recyclerView.adapter = adapter

        SingletonKotlin.loadUserCoins(coinText)

        // 현재 장착중인 배경으로 배경 설정
        SingletonKotlin.loadUserBackground(frame)
        // 현재 장착중인 고양이 친구 불러오기
        SingletonKotlin.loadUserCatFriend(newCatGif)

        buyBtn.setOnClickListener {
            Log.d("ShopActivity", "Buy button clicked")
            checkItemAlreadyPurchased(clickedItem)
        }

        roomBtn.setOnClickListener {
            Log.d("ShopActivity", "Room button clicked")
            val intent = Intent(this, CatRoomActivity::class.java)
            startActivity(intent)
            finish()
        }
        decoBtn.setOnClickListener {
            Log.d("ShopActivity", "Deco button clicked")
            val intent = Intent(this, DecoActivity::class.java)
            startActivity(intent)
            finish()
        }
        gameBtn.setOnClickListener {
            Log.d("ShopActivity", "Game button clicked")
            val intent = Intent(this, GamelistActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onItemClick(position: Int) {
        Log.d("ShopActivity", "Item clicked at position: $position")
        clickedItem = shopItemList[position]
        if (clickedItem.name.startsWith("Cat Friend")) {
            val newCatGif = findViewById<GifImageView>(R.id.newcat_gif)
            newCatGif.setImageResource(clickedItem.imageResource)
            (newCatGif.drawable as? GifDrawable)?.apply {
                loopCount = 0 // 무한 반복
                start()
            }
            newCatGif.visibility = View.VISIBLE
        } else if (clickedItem.name == "No Cat Friend") {
            val newCatGif = findViewById<GifImageView>(R.id.newcat_gif)
            newCatGif.visibility = View.GONE
        } else {
            frame.setBackgroundResource(clickedItem.imageResource)
        }
        buyBtn.visibility = View.VISIBLE
        buyLayout.visibility = View.VISIBLE
    }

    private fun checkItemAlreadyPurchased(clickedItem: ShopItem) { // 구매했는지를 확인하는 함수
        val currentUser = auth.currentUser
        currentUser?.let {
            val userItemsRef = if (clickedItem.name.startsWith("Cat Friend")) {
                database.child("user_cat_friends").child(it.uid).child(clickedItem.name)
            } else {
                database.child("user_rooms").child(it.uid).child(clickedItem.name)
            }

            userItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { // SNAPSHOT을 통해 DB에 존재하는지를 비교하여 확인
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

    private fun showAlreadyPurchasedDialog() {
        val confirmDialog = ConfirmDialog(
            object : DialogCustomInterface {
                override fun onClickYesButton(id: Int) {
                }
            },
            title = "Item Already Purchased",
            content = "You have already purchased this item",
            buttonText = "OK",
            id = -1
        )

        confirmDialog.isCancelable = false
        confirmDialog.show(supportFragmentManager, "ConfirmDialog")
    }
    private fun showPurchaseConfirmationDialog(clickedItem: ShopItem)  {
        val confirmDialog = ConfirmDialog(
            object : DialogCustomInterface {
                override fun onClickYesButton(id: Int) {
                    handlePurchase(clickedItem)
                }
            },
            title = "Purchase Confirmation",
            content = "Do you want to buy ${clickedItem.name} for ${clickedItem.price}?",
            buttonText = "Buy",
            id = 1
        )

        confirmDialog.isCancelable = false
        confirmDialog.show(supportFragmentManager, "ConfirmDialog")
    }

    private fun showNotEnoughCoinsDialog(clickedItem: ShopItem) {
        val confirmDialog = ConfirmDialog(
            object : DialogCustomInterface {
                override fun onClickYesButton(id: Int) {
                }
            },
            title = "Not Enough Coins",
            content = "You don't have enough coins to buy ${clickedItem.name}.",
            buttonText = "OK",
            id = -1
        )

        confirmDialog.isCancelable = false
        confirmDialog.show(supportFragmentManager, "ConfirmDialog")
    }

    private fun handlePurchase(clickedItem: ShopItem) {
        val itemPrice = clickedItem.price  // 가격을 직접 사용
        val userCoins = coinText.text.toString().toInt()  // 사용자 보유 코인 가져오기

        if (userCoins < itemPrice) {
            showNotEnoughCoinsDialog(clickedItem) // 코인 부족할 때
        } else {
            // 구매 후 코인 차감 예시
            val remainingCoins = userCoins - itemPrice
            coinText.text = remainingCoins.toString()  // 화면에 남은 코인을 업데이트

            val currentUser = auth.currentUser // 현재 유저 로그인 정보를 불러오고
            currentUser?.let { // 이상 없이 유저 존재한다면
                val userRef = database.child("users").child(it.uid) // UID 불러와서
                userRef.child("coins").setValue(remainingCoins) // DB에 코인 업데이트

                val userItemsRef = if (clickedItem.name.startsWith("Cat Friend")) {
                    database.child("user_cat_friends").child(it.uid).child(clickedItem.name)
                } else {
                    database.child("user_rooms").child(it.uid).child(clickedItem.name)
                }
                val purchaseData = mapOf( // 아이템을 구매했는지 여부와 가격을 저장하도록 함.
                    "purchased" to true,
                    "price" to itemPrice
                )
                userItemsRef.setValue(purchaseData) // 아이템 구매 정보 및 가격 저장

                if (clickedItem.name.startsWith("Cat Friend") || clickedItem.name == "No Cat Friend") {
                    SingletonKotlin.saveUserCatFriend(clickedItem.name)
                } else {
                    SingletonKotlin.saveUserBackground(clickedItem.name)
                }

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

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(applicationContext, CatRoomActivity::class.java)
        startActivity(intent)
        finish()
    }
}
