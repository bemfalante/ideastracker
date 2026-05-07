package com.jules.ideastracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jules.ideastracker.databinding.FragmentIdeasBinding
import java.io.File
import java.io.FileOutputStream

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
            onMove = { idea, newStatus -> viewModel.updateStatus(idea, newStatus) },
            onDelete = { idea -> viewModel.delete(idea) },
            onClick = { idea -> showEditDialog(idea) },
            onShare = { idea -> shareAsTextFile(idea) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        viewModel.getIdeas(status).observe(viewLifecycleOwner) { ideas ->
            adapter.submitList(ideas)
        }
    }

    private fun shareAsTextFile(idea: Idea) {
        val fileName = "${idea.title.replace(" ", "_")}.txt"
        val content = """
            Title: ${idea.title}
            Category: ${idea.category ?: "N/A"}
            Description: ${idea.description}
            Link: ${idea.link ?: "N/A"}
            Status: ${idea.status}
            Definition of Done: ${idea.definitionOfDone ?: "N/A"}
            Next Steps: ${idea.nextSteps ?: "N/A"}
        """.trimIndent()

        try {
            val file = File(requireContext().cacheDir, fileName)
            FileOutputStream(file).use {
                it.write(content.toByteArray())
            }

            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Idea via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showEditDialog(idea: Idea) {
        val dialog = AddIdeaDialog.newInstance(idea)
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
