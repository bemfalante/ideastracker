package com.example.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideastracker.databinding.FragmentIdeasBinding

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
            onClick = { idea -> showEditDialog(idea) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        viewModel.getIdeas(status).observe(viewLifecycleOwner) { ideas ->
            adapter.submitList(ideas)
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
