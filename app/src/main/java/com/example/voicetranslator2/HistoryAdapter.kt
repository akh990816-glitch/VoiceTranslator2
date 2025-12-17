package com.example.voicetranslator2


import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// UI에서 사용할 데이터 모델
data class HistoryItem(
    val original: String,
    val translated: String,
    val langCode: String,
    val timestamp: String
)

class HistoryAdapter(private val items: MutableList<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrig: TextView = view.findViewById(android.R.id.text1)
        val tvTrans: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 안드로이드 기본 레이아웃 사용
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvOrig.text = "🇰🇷: ${item.original}"
        holder.tvTrans.text = "🌐: ${item.translated} (${item.langCode.uppercase()})"

        // 클릭 시 공유하기 기능
        holder.itemView.setOnClickListener {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "[번역기록]\n원문: ${item.original}\n번역: ${item.translated}")
                type = "text/plain"
            }
            holder.itemView.context.startActivity(Intent.createChooser(sendIntent, "공유하기"))
        }
    }

    override fun getItemCount() = items.size

    fun addItem(item: HistoryItem) {
        items.add(0, item)
        notifyItemInserted(0)
    }
}