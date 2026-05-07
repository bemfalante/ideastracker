package com.jules.ideastracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jules.ideastracker.databinding.FragmentIdeasBinding

class IdeasFragment : Fragment() {

    private var _binding: FragmentIdeasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IdeaViewModel by activityViewModels {
        IdeaViewModelFactory(AppDatabase.getDatabase(requireContext()).ideaDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIdeasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val status = arguments?.getSerializable("status") as IdeaStatus
        val adapter = IdeaAdapter(
            onMove = { idea, newStatus ->
                if (newStatus == IdeaStatus.ONGOING || newStatus == IdeaStatus.DONE) {
                    // Rule 5 & 8: Prompt user when starting or finishing
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

        viewModel.getIdeas(status).observe(viewLifecycleOwner) { ideas ->
            adapter.submitList(ideas)
        }
    }

    private fun shareAsText(idea: Idea) {
        val content = """
            TITLE: ${idea.title}
            CATEGORY: ${idea.category ?: "Uncategorized"}
            DESCRIPTION: ${idea.description}
            LINK: ${idea.link ?: "N/A"}
            STATUS: ${idea.status}
            DEFINITION OF DONE: ${idea.definitionOfDone ?: "N/A"}
            NEXT STEPS: ${idea.nextSteps ?: "N/A"}
            CONCLUSION: ${idea.conclusion ?: "N/A"}
            CREATED AT: ${idea.timestamp}
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Idea: ${idea.title}")
                putExtra(Intent.EXTRA_TEXT, content)
            }
            startActivity(Intent.createChooser(intent, "Share Idea via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showEditDialog(idea: Idea, promptForMove: Boolean = false, targetStatus: IdeaStatus? = null) {
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
