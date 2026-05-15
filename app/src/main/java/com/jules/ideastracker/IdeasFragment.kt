package com.jules.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jules.ideastracker.databinding.FragmentIdeasBinding
import java.util.*

class IdeasFragment : Fragment() {

    private var _binding: FragmentIdeasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IdeaViewModel by activityViewModels {
        IdeaViewModelFactory(AppDatabase.getDatabase(requireContext()).ideaDao())
    }

    private lateinit var adapter: IdeaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIdeasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val status = arguments?.getSerializable("status") as IdeaStatus
        adapter = IdeaAdapter(
            onMove = { idea, newStatus ->
                if (newStatus == IdeaStatus.ONGOING || newStatus == IdeaStatus.DONE) {
                    showEditDialog(idea, true, newStatus)
                } else {
                    viewModel.updateStatus(idea, newStatus)
                }
            },
            onDelete = { idea -> viewModel.delete(idea) },
            onClick = { idea -> showEditDialog(idea) },
            onShare = { idea -> shareAsText(idea) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Rule 2: Drag and Drop
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                val list = adapter.currentList.toMutableList()
                Collections.swap(list, fromPos, toPos)
                adapter.submitList(list)
                viewModel.updateManualOrder(list)
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.getIdeas(status).observe(viewLifecycleOwner) { ideas ->
            adapter.submitList(ideas)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    // [Rest of shareAsText and showEditDialog same as before]
    private fun shareAsText(idea: Idea) {
        val content = "TITLE: ${idea.title}\nCATEGORY: ${idea.category ?: "Uncategorized"}\nDESCRIPTION: ${idea.description}\nLINK: ${idea.link ?: "N/A"}\nSTATUS: ${idea.status}\nDEFINITION OF DONE: ${idea.definitionOfDone ?: "N/A"}\nNEXT STEPS: ${idea.nextSteps ?: "N/A"}\nCONCLUSION: ${idea.conclusion ?: "N/A"}"
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Idea: ${idea.title}")
                putExtra(android.content.Intent.EXTRA_TEXT, content)
            }
            startActivity(android.content.Intent.createChooser(intent, "Share Idea via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showEditDialog(idea: Idea, promptForMove: Boolean = false, targetStatus: IdeaStatus? = null) {
        if (parentFragmentManager.findFragmentByTag("EditIdeaDialog") != null) return
        val dialog = AddIdeaDialog.newInstance(idea, promptForMove, targetStatus)
        dialog.show(parentFragmentManager, "EditIdeaDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(status: IdeaStatus) = IdeasFragment().apply {
            arguments = Bundle().apply {
                putSerializable("status", status)
            }
        }
    }
}
