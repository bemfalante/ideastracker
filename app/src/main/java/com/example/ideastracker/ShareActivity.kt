package com.example.ideastracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ideastracker.databinding.ActivityShareBinding
import kotlinx.coroutines.launch

class ShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getDatabase(this).ideaDao()

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleSendText(intent)
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val link = binding.etLink.text.toString().takeIf { it.isNotBlank() }

            if (title.isNotBlank()) {
                lifecycleScope.launch {
                    dao.insert(Idea(title = title, description = description, link = link, status = IdeaStatus.FUTURE))
                    finish()
                }
            } else {
                binding.etTitle.error = "Title is required"
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
            binding.etLink.setText(sharedText)
            // If it's a URL, we might want to put it in the link field.
            // Often social media sharing includes some text + URL.
        }
    }
}
