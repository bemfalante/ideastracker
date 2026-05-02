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

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val link = binding.etLink.text.toString().takeIf { it.isNotBlank() }

            if (title.isNotBlank()) {
                viewModel.insert(Idea(title = title, description = description, link = link, status = IdeaStatus.ONGOING))
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
}
