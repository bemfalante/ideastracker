package com.jules.ideastracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jules.ideastracker.databinding.ActivityNotesBinding
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(AppDatabase.getDatabase(this).noteDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = NotesAdapter(
            onDelete = { note -> viewModel.delete(note) },
            onLongClick = { note -> showEditNoteDialog(note) }
        )
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter

        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)
        }

        binding.btnAddNote.setOnClickListener {
            val content = binding.etNote.text.toString()
            if (content.isNotBlank()) {
                viewModel.insert(Note(content = content))
                binding.etNote.setText("")
            }
        }
    }

    private fun showEditNoteDialog(note: Note) {
        val editText = EditText(this)
        editText.setText(note.content)

        AlertDialog.Builder(this)
            .setTitle("Edit Note")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString()
                if (newContent.isNotBlank()) {
                    viewModel.update(note.copy(content = newContent))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class NotesViewModel(private val dao: NoteDao) : ViewModel() {
    val allNotes = dao.getAllNotes()

    fun insert(note: Note) = viewModelScope.launch { dao.insert(note) }
    fun update(note: Note) = viewModelScope.launch { dao.update(note) }
    fun delete(note: Note) = viewModelScope.launch { dao.delete(note) }
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
