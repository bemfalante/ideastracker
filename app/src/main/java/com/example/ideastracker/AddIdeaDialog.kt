package com.example.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.ideastracker.databinding.DialogAddIdeaBinding

class AddIdeaDialog : DialogFragment() {

    private var _binding: DialogAddIdeaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IdeaViewModel by activityViewModels {
        IdeaViewModelFactory(AppDatabase.getDatabase(requireContext()).ideaDao())
    }

    private var existingIdea: Idea? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        existingIdea = arguments?.getSerializable(ARG_IDEA) as? Idea
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddIdeaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        existingIdea?.let { idea ->
            binding.tvDialogTitle.text = "Edit Idea"
            binding.etTitle.setText(idea.title)
            binding.etCategory.setText(idea.category)
            binding.etDescription.setText(idea.description)
            binding.etLink.setText(idea.link)
            binding.etDoD.setText(idea.definitionOfDone)
            binding.etNextSteps.setText(idea.nextSteps)
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val category = binding.etCategory.text.toString().takeIf { it.isNotBlank() }
            val description = binding.etDescription.text.toString()
            val link = binding.etLink.text.toString().takeIf { it.isNotBlank() }
            val dod = binding.etDoD.text.toString().takeIf { it.isNotBlank() }
            val nextSteps = binding.etNextSteps.text.toString().takeIf { it.isNotBlank() }

            if (title.isNotBlank()) {
                if (existingIdea != null) {
                    viewModel.update(existingIdea!!.copy(
                        title = title,
                        category = category,
                        description = description,
                        link = link,
                        definitionOfDone = dod,
                        nextSteps = nextSteps
                    ))
                } else {
                    viewModel.insert(Idea(
                        title = title,
                        category = category,
                        description = description,
                        link = link,
                        status = IdeaStatus.FUTURE // Changed to FUTURE
                    ))
                }
                dismiss()
            } else {
                binding.etTitle.error = "Title is required"
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IDEA = "idea"

        fun newInstance(idea: Idea? = null) = AddIdeaDialog().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_IDEA, idea)
            }
        }
    }
}
