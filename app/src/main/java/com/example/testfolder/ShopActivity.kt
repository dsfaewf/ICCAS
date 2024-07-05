package com.example.testfolder

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var buyLayout: LinearLayout
    private lateinit var clickedItem: ShopItem

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

//        auth = FirebaseAuth.getInstance()
//        database = FirebaseDatabase.getInstance().reference
//        SingletonKotlin.initialize(auth, database)
//        아직 페이지에서 직접적인 사용이 없으므로 주석처리하겠음.
//        auth = SingletonKotlin.getAuth()
//        database = SingletonKotlin.getDatabase()

        coinText = findViewById(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        // GIF 반복 설정
        val gifDrawable = catGif.drawable as GifDrawable
        frame = findViewById(R.id.shop_frame)
        buyBtn = findViewById(R.id.buy_button)
        buyLayout = findViewById(R.id.buy_layout)
        gifDrawable.loopCount = 0 // 무한 반복

        recyclerView = findViewById(R.id.shop_items_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        shopItemList = listOf(
            ShopItem(R.drawable.room01, "Item 1", "$0"),
            ShopItem(R.drawable.room02, "Item 2", "$0"),
            ShopItem(R.drawable.room03, "Item 3", "$0"),
            ShopItem(R.drawable.room04, "Item 4", "$0"),
            ShopItem(R.drawable.room05, "Item 5", "$0"),
            ShopItem(R.drawable.room06, "Item 6", "$0"),
            ShopItem(R.drawable.room07, "Item 7", "$0"),
            ShopItem(R.drawable.room08, "Item 8", "$0"),
        )

        adapter = ShopItemsAdapter(shopItemList, this, this)
        recyclerView.adapter = adapter

        // 사용자 코인 불러오기 - 싱글톤으로 수정!
        SingletonKotlin.loadUserCoins(coinText)

        buyBtn.setOnClickListener {
            showPurchaseConfirmationDialog(clickedItem)
        }
    }

    override fun onItemClick(position: Int) {
        clickedItem = shopItemList[position]
        frame.setBackgroundResource(clickedItem.imageResource)
        buyBtn.visibility = View.VISIBLE
        buyLayout.visibility = View.VISIBLE
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
        val itemPrice = clickedItem.price.substring(1).toInt()  // 가격 문자열에서 "$" 제거 후 정수로 변환
        val userCoins = coinText.text.toString().toInt()  // 사용자 보유 코인 가져오기

        if (userCoins < itemPrice) {
            showNotEnoughCoinsDialog(clickedItem) // 코인 부족할 때
        } else {
            // 여기에 아이템을 구매하는 코드를 작성합니다.
            // 예를 들어, 구매한 아이템을 데이터베이스에 업데이트하거나, 코인을 차감하는 등의 작업을 수행할 수 있습니다.

            // 구매 후 코인 차감 예시
            val remainingCoins = userCoins - itemPrice
            coinText.text = remainingCoins.toString()  // 화면에 남은 코인을 업데이트 -> DB 업데이트 해야해용

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Purchase Successful")
                .setMessage("You bought ${clickedItem.name} for ${clickedItem.price}.")
                .setPositiveButton("OK") { dialog, which ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}
