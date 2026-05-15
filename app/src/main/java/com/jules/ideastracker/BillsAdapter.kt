package com.jules.ideastracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jules.ideastracker.databinding.ItemBillBinding
import java.text.SimpleDateFormat
import java.util.*

class BillsAdapter(
    private val onCheckChanged: (Bill, Boolean) -> Unit,
    private val onLongClick: (Bill) -> Unit
) : ListAdapter<Bill, BillsAdapter.BillViewHolder>(BillDiffCallback()) {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BillViewHolder(private val binding: ItemBillBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bill: Bill) {
            binding.cbBill.text = "${bill.title} (${sdf.format(Date(bill.dueDate))})"
            binding.cbBill.setOnCheckedChangeListener(null)
            binding.cbBill.isChecked = bill.isPaid
            binding.cbBill.setOnCheckedChangeListener { _, isChecked -> onCheckChanged(bill, isChecked) }

            binding.root.setOnLongClickListener {
                onLongClick(bill)
                true
            }
        }
    }

    class BillDiffCallback : DiffUtil.ItemCallback<Bill>() {
        override fun areItemsTheSame(oldItem: Bill, newItem: Bill): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bill, newItem: Bill): Boolean = oldItem == newItem
    }
}
