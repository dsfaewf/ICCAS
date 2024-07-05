package com.example.testfolder

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class DecoActivity : AppCompatActivity(),ShopItemsAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopItemsAdapter
    private lateinit var buyItemList: List<ShopItem>
    private lateinit var coinText: TextView
    private lateinit var frame: FrameLayout
    private lateinit var roomBtn: TextView
    private lateinit var shopBtn: TextView
    private lateinit var saveBtn: TextView
    private lateinit var clickedItem: ShopItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deco)

        coinText = findViewById(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        val gifDrawable = catGif.drawable as GifDrawable
        frame = findViewById(R.id.deco_frame)
        roomBtn = findViewById(R.id.room_btn_deco)
        shopBtn = findViewById(R.id.shop_btn_deco)
        saveBtn = findViewById(R.id.save_btn_deco)
        gifDrawable.loopCount = 0 // 무한 반복

        recyclerView = findViewById(R.id.buy_items_recycler_view)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // db에서 불러오는 방법으로 수정해야할 것 같아요
        buyItemList = listOf(
            ShopItem(R.drawable.room01, "Item 1", "$0"),
        )
        adapter = ShopItemsAdapter(buyItemList, this, this)
        recyclerView.adapter = adapter


        roomBtn.setOnClickListener {
            val intent = Intent(this, CatRoomActivity::class.java)
            startActivity(intent)
        }
        shopBtn.setOnClickListener {
            val intent = Intent(this, ShopActivity::class.java)
            startActivity(intent)
        }
        saveBtn.setOnClickListener {
            // db 저장
            showConfirmationDialog() // 팝업 띄우고 확인 누르면 룸 이동
        }
    }

    override fun onItemClick(position: Int) {
        clickedItem = buyItemList[position]
        frame.setBackgroundResource(clickedItem.imageResource)
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Save Confirmation")
            .setMessage("Do you want to save and move to the room?")
            .setPositiveButton("Yes") { dialog, which ->
                val intent = Intent(this, CatRoomActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

}