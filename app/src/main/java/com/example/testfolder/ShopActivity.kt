package com.example.testfolder

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

class ShopActivity : AppCompatActivity(), ShopItemsAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopItemsAdapter
    private lateinit var shopItemList: List<ShopItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        var coin = findViewById<TextView>(R.id.coin_text)
        val catGif = findViewById<GifImageView>(R.id.cat_gif)
        // GIF 반복 설정
        val gifDrawable = catGif.drawable as GifDrawable
        gifDrawable.loopCount = 0 // 무한 반복

        coin.setText("1000")    // user coin 개수

        recyclerView = findViewById(R.id.shop_items_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        shopItemList = listOf(
            ShopItem(R.drawable.sample_item, "Item 1", "$10"),
            ShopItem(R.drawable.sample_item, "Item 2", "$20"),
            ShopItem(R.drawable.sample_item, "Item 3", "$30")
        )

        adapter = ShopItemsAdapter(shopItemList, this, this)
        recyclerView.adapter = adapter
    }

    override fun onItemClick(position: Int) {
        val clickedItem = shopItemList[position]
        Toast.makeText(this, "Clicked: ${clickedItem.name} - ${clickedItem.price}", Toast.LENGTH_SHORT).show()
    }
}
