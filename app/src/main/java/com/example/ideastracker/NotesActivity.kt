package com.example.ideastracker

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideastracker.databinding.ActivityNotesBinding
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

        val adapter = NotesAdapter { note -> viewModel.delete(note) }
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
}

class NotesViewModel(private val dao: NoteDao) : ViewModel() {
    val allNotes = dao.getAllNotes()

    fun insert(note: Note) = viewModelScope.launch { dao.insert(note) }
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
