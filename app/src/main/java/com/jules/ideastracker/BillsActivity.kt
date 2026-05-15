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
import com.jules.ideastracker.databinding.ActivityBillsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillsBinding
    private val viewModel: BillsViewModel by viewModels {
        BillsViewModelFactory(AppDatabase.getDatabase(this).billDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = BillsAdapter(
            onCheckChanged = { bill, isChecked -> viewModel.update(bill.copy(isPaid = isChecked)) },
            onLongClick = { bill -> showBillCrudDialog(bill) }
        )
        binding.rvBills.layoutManager = LinearLayoutManager(this)
        binding.rvBills.adapter = adapter

        viewModel.allBills.observe(this) { bills ->
            adapter.submitList(bills)
        }

        binding.btnAddBill.setOnClickListener {
            showBillCrudDialog()
        }
    }

    private fun showBillCrudDialog(bill: Bill? = null) {
        val view = layoutInflater.inflate(R.layout.dialog_bill_crud, null)
        val etTitle = view.findViewById<EditText>(R.id.etBillTitle)
        val etDate = view.findViewById<EditText>(R.id.etBillDate) // Format simple DD/MM/YYYY

        bill?.let {
            etTitle.setText(it.title)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etDate.setText(sdf.format(Date(it.dueDate)))
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (bill == null) "Add Bill" else "Edit Bill")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString()
                val dateStr = etDate.text.toString()
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
                    if (bill == null) {
                        viewModel.insert(Bill(title = title, dueDate = date))
                    } else {
                        viewModel.update(bill.copy(title = title, dueDate = date))
                    }
                } catch (e: Exception) { /* ignore */ }
            }

        if (bill != null) {
            builder.setNeutralButton("Delete") { _, _ -> viewModel.delete(bill) }
        }

        builder.setNegativeButton("Cancel", null).show()
    }
}

class BillsViewModel(private val dao: BillDao) : ViewModel() {
    val allBills = dao.getAllBills()
    fun insert(bill: Bill) = viewModelScope.launch { dao.insert(bill) }
    fun update(bill: Bill) = viewModelScope.launch { dao.update(bill) }
    fun delete(bill: Bill) = viewModelScope.launch { dao.delete(bill) }
}

class BillsViewModelFactory(private val dao: BillDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillsViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
