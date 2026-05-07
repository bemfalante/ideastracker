package com.jules.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.jules.ideastracker.databinding.DialogAddIdeaBinding

class AddIdeaDialog : DialogFragment() {

    private var _binding: DialogAddIdeaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IdeaViewModel by activityViewModels {
        IdeaViewModelFactory(AppDatabase.getDatabase(requireContext()).ideaDao())
    }

    private var existingIdea: Idea? = null
    private var promptForMove: Boolean = false
    private var targetStatus: IdeaStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        existingIdea = arguments?.getSerializable(ARG_IDEA) as? Idea
        promptForMove = arguments?.getBoolean(ARG_PROMPT_MOVE) ?: false
        targetStatus = arguments?.getSerializable(ARG_TARGET_STATUS) as? IdeaStatus
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

        val isNew = existingIdea == null
        val status = targetStatus ?: existingIdea?.status ?: IdeaStatus.FUTURE

        // Rule 5: Show special fields only if Ongoing or Done (or moving to them)
        binding.specialFieldsContainer.visibility = if (status == IdeaStatus.FUTURE) View.GONE else View.VISIBLE
        binding.tilConclusion.visibility = if (status == IdeaStatus.DONE) View.VISIBLE else View.GONE

        if (promptForMove) {
            binding.tvDialogTitle.text = when(targetStatus) {
                IdeaStatus.ONGOING -> "Start Idea: Complete Special Fields"
                IdeaStatus.DONE -> "Finish Idea: Enter Conclusion"
                else -> "Edit Idea"
            }
        }

        existingIdea?.let { idea ->
            if (!promptForMove) binding.tvDialogTitle.text = "Edit Idea"
            binding.etTitle.setText(idea.title)
            binding.etCategory.setText(idea.category)
            binding.etDescription.setText(idea.description)
            binding.etLink.setText(idea.link)
            binding.etDoD.setText(idea.definitionOfDone)
            binding.etNextSteps.setText(idea.nextSteps)
            binding.etConclusion.setText(idea.conclusion)
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val category = binding.etCategory.text.toString().takeIf { it.isNotBlank() }
            val description = binding.etDescription.text.toString()
            val link = binding.etLink.text.toString().takeIf { it.isNotBlank() }
            val dod = binding.etDoD.text.toString()
            val nextSteps = binding.etNextSteps.text.toString()
            val conclusion = binding.etConclusion.text.toString()

            if (title.isNotBlank()) {
                if (promptForMove && existingIdea != null && targetStatus != null) {
                    viewModel.updateStatus(existingIdea!!, targetStatus!!, dod, nextSteps, conclusion)
                } else if (existingIdea != null) {
                    viewModel.update(existingIdea!!.copy(
                        title = title,
                        category = category,
                        description = description,
                        link = link,
                        definitionOfDone = dod,
                        nextSteps = nextSteps,
                        conclusion = conclusion
                    ))
                } else {
                    viewModel.insert(Idea(
                        title = title,
                        category = category,
                        description = description,
                        link = link,
                        status = IdeaStatus.FUTURE
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
        private const val ARG_PROMPT_MOVE = "prompt_move"
        private const val ARG_TARGET_STATUS = "target_status"

        fun newInstance(idea: Idea? = null, promptForMove: Boolean = false, targetStatus: IdeaStatus? = null) = AddIdeaDialog().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_IDEA, idea)
                putBoolean(ARG_PROMPT_MOVE, promptForMove)
                putSerializable(ARG_TARGET_STATUS, targetStatus)
            }
        }
    }
}
