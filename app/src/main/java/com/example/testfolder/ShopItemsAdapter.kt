package com.example.testfolder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShopItemsAdapter(private val shopItems: List<ShopItem>, private val context: Context, private val itemClickListener: OnItemClickListener) : RecyclerView.Adapter<ShopItemsAdapter.ShopItemViewHolder>() {

    // Define the listener interface
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.shop_item_layout, parent, false)
        return ShopItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopItemViewHolder, position: Int) {
        val shopItem = shopItems[position]
        holder.itemImage.setImageResource(shopItem.imageResource)
        holder.itemName.text = shopItem.name
        holder.itemPrice.text = shopItem.price

        // Set click listener
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return shopItems.size
    }

    class ShopItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        val itemName: TextView = itemView.findViewById(R.id.item_name)
        val itemPrice: TextView = itemView.findViewById(R.id.item_price)
    }
}
