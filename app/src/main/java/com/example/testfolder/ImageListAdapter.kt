package com.example.testfolder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

data class ImageData(
    val imageUrl: String,
    val keyword: String?,
    val dateTime: String?,
    val gpsLatitude: String?,
    val gpsLongitude: String?
)

class ImageListAdapter(private val context: Context, private val imageList: List<ImageData>) : BaseAdapter() {

    override fun getCount(): Int {
        return imageList.size
    }

    override fun getItem(position: Int): Any {
        return imageList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.image_list_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val imageData = imageList[position]
        holder.keywordTextView.text = imageData.keyword
        holder.dateTimeTextView.text = imageData.dateTime
        holder.imageView.clipToOutline = true

        Glide.with(context)
            .load(imageData.imageUrl)
            .centerCrop()
            .into(holder.imageView)

        return view
    }

    private class ViewHolder(view: View) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val keywordTextView: TextView = view.findViewById(R.id.keywordTextView)
        val dateTimeTextView: TextView = view.findViewById(R.id.dateTimeTextView)
    }
}
