package com.example.barcodescanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemScanBinding

/** One row that will become one line of the exported CSV. */
data class ScanRecord(val value: String, val format: String, val timestamp: String)

class ScanAdapter : RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    private val items = mutableListOf<ScanRecord>()

    fun addItem(record: ScanRecord) {
        items.add(record)
        notifyItemInserted(items.size - 1)
    }

    fun clearAll() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun currentItems(): List<ScanRecord> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemScanBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: ScanRecord) {
            binding.tvValue.text = record.value
            binding.tvMeta.text = "${record.format} • ${record.timestamp}"
        }
    }
}
