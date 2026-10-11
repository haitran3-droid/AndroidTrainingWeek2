package com.example.aquadaily.ui.report

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aquadaily.R
import com.example.aquadaily.data.DrinkEntry
import com.example.aquadaily.databinding.ItemDrinkBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Adapter hiển thị danh sách các lần uống nước trong một ngày cụ thể.
 *
 * @param onDelete Callback được gọi khi người dùng nhấn nút xóa một lần uống.
 */
class DrinkHistoryAdapter(private val onDelete: (DrinkEntry, String) -> Unit) :
    ListAdapter<DrinkEntry, DrinkHistoryAdapter.Holder>(Diff) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    class Holder(val ui: ItemDrinkBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemDrinkBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        // Định dạng mốc thời gian dạng epoch milli thành giờ:phút (HH:mm) theo múi giờ hệ thống
        val time = Instant.ofEpochMilli(row.timestamp).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        holder.ui.amount.text = holder.itemView.context.getString(R.string.amount_ml, row.amountMl)
        holder.ui.time.text = time
        holder.ui.delete.contentDescription =
            holder.itemView.context.getString(R.string.delete_accessibility, row.amountMl, time)
        holder.ui.delete.setOnClickListener { onDelete(row, time) }
    }

    private object Diff : DiffUtil.ItemCallback<DrinkEntry>() {
        override fun areItemsTheSame(oldItem: DrinkEntry, newItem: DrinkEntry) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DrinkEntry, newItem: DrinkEntry) =
            oldItem == newItem
    }
}

