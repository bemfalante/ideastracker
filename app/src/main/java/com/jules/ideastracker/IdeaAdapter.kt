package com.jules.ideastracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jules.ideastracker.databinding.ItemIdeaBinding
import java.text.SimpleDateFormat
import java.util.*

class IdeaAdapter(
    private val onMove: (Idea, IdeaStatus) -> Unit,
    private val onDelete: (Idea) -> Unit,
    private val onClick: (Idea) -> Unit,
    private val onShare: (Idea) -> Unit,
    private val onMoveUp: (Idea) -> Unit,
    private val onMoveDown: (Idea) -> Unit
) : ListAdapter<Idea, IdeaAdapter.IdeaViewHolder>(IdeaDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdeaViewHolder {
        val binding = ItemIdeaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IdeaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IdeaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IdeaViewHolder(private val binding: ItemIdeaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(idea: Idea) {
            binding.tvTitle.text = idea.title
            binding.tvCategory.text = idea.category ?: "Uncategorized"
            binding.tvDescription.text = idea.description
            binding.tvLink.text = idea.link
            binding.tvLink.visibility = if (idea.link.isNullOrEmpty()) View.GONE else View.VISIBLE

            if (idea.status == IdeaStatus.DONE) {
                binding.tvDoD.visibility = if (idea.definitionOfDone.isNullOrEmpty()) View.GONE else View.VISIBLE
                binding.tvDoD.text = "Definition of Done: ${idea.definitionOfDone}"
                binding.tvNextSteps.visibility = View.GONE
                binding.tvConclusion.visibility = if (idea.conclusion.isNullOrEmpty()) View.GONE else View.VISIBLE
                binding.tvConclusion.text = "Conclusion: ${idea.conclusion}"
            } else {
                binding.tvDoD.visibility = if (idea.definitionOfDone.isNullOrEmpty()) View.GONE else View.VISIBLE
                binding.tvDoD.text = "Definition of Done: ${idea.definitionOfDone}"
                binding.tvNextSteps.visibility = if (idea.nextSteps.isNullOrEmpty()) View.GONE else View.VISIBLE
                binding.tvNextSteps.text = "Next Steps: ${idea.nextSteps}"
                binding.tvConclusion.visibility = View.GONE
            }

            val dateText = StringBuilder()
            dateText.append("Created: ${dateFormat.format(Date(idea.timestamp))}")
            idea.startedTimestamp?.let { dateText.append("\nStarted: ${dateFormat.format(Date(it))}") }
            idea.finishedTimestamp?.let { dateText.append("\nFinished: ${dateFormat.format(Date(it))}") }

            if (idea.status == IdeaStatus.ONGOING) {
                idea.lastSavedTimestamp?.let { dateText.append("\nLast Saved: ${dateFormat.format(Date(it))}") }
            }

            binding.tvDates.text = dateText.toString()

            when (idea.status) {
                IdeaStatus.FUTURE -> {
                    binding.btnLeft.visibility = View.GONE
                    binding.btnRight.visibility = View.VISIBLE
                    binding.btnRight.text = "Start"
                    binding.btnRight.setOnClickListener { onMove(idea, IdeaStatus.ONGOING) }
                }
                IdeaStatus.ONGOING -> {
                    binding.btnLeft.visibility = View.VISIBLE
                    binding.btnLeft.text = "Revert"
                    binding.btnLeft.setOnClickListener { onMove(idea, IdeaStatus.FUTURE) }
                    binding.btnRight.visibility = View.VISIBLE
                    binding.btnRight.text = "Finish"
                    binding.btnRight.setOnClickListener { onMove(idea, IdeaStatus.DONE) }
                }
                IdeaStatus.DONE -> {
                    binding.btnLeft.visibility = View.VISIBLE
                    binding.btnLeft.text = "Revert"
                    binding.btnLeft.setOnClickListener { onMove(idea, IdeaStatus.ONGOING) }
                    binding.btnRight.visibility = View.GONE
                }
            }

            binding.btnDelete.setOnClickListener { onDelete(idea) }
            binding.btnShare.setOnClickListener { onShare(idea) }

            // Rule 2: Move Up and Move Down
            binding.btnToTop.setOnClickListener { onMoveUp(idea) }
            binding.btnToBottom.setOnClickListener { onMoveDown(idea) }

            binding.itemContainer.setOnClickListener { onClick(idea) }
            binding.itemContainer.setOnLongClickListener {
                onClick(idea)
                true
            }
        }
    }

    class IdeaDiffCallback : DiffUtil.ItemCallback<Idea>() {
        override fun areItemsTheSame(oldItem: Idea, newItem: Idea): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Idea, newItem: Idea): Boolean = oldItem == newItem
    }
}
