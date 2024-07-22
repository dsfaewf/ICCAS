package com.katzheimer.testfolder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

data class DiaryData(
    val diaryId: String,
    val content: String?,
    val date: String?,
)

class DiaryListAdapter(private val context: Context, private val diaryList: List<DiaryData>) : BaseAdapter() {

    override fun getCount(): Int {
        return diaryList.size
    }

    override fun getItem(position: Int): Any {
        return diaryList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.diary_list_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val diaryData = diaryList[position]
        holder.date.text = diaryData.date
        holder.content.text =  diaryData.content

        return view
    }

    private class ViewHolder(view: View) {
        val date: TextView = view.findViewById(R.id.edit_date)
        val content: TextView = view.findViewById(R.id.edit_content)
    }
}
