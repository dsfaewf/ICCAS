package com.example.testfolder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class ImageListAdapter(context: Context, private val imageList: List<ImageListActivity.ImageData>)
    : ArrayAdapter<ImageListActivity.ImageData>(context, 0, imageList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_image, parent, false)
        val imageData = imageList[position]

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.textView)

        Glide.with(context).load(imageData.imageUrl).into(imageView)
        textView.text = imageData.toString()

        return view
    }
}
