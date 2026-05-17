package com.jules.ideastracker

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jules.ideastracker.databinding.ItemBillBinding
import java.text.SimpleDateFormat
import java.util.*

class BillsAdapter(
    private val onClick: (Bill) -> Unit,
    private val onDelete: (Bill) -> Unit,
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
            binding.tvBillTitle.text = "${bill.title} (${sdf.format(Date(bill.dueDate))})"

            if (bill.isPaid) {
                binding.tvBillTitle.paintFlags = binding.tvBillTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvBillTitle.paintFlags = binding.tvBillTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.tvBillTitle.setOnClickListener { onClick(bill) }
            binding.btnDeleteBill.setOnClickListener { onDelete(bill) }

            binding.root.setOnLongClickListener {
                onLongClick(bill)
                true
            }
            binding.tvBillTitle.setOnLongClickListener {
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
