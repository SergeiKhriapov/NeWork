package ru.netology.nework.ui.users

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.netology.nework.R
import ru.netology.nework.databinding.DialogDatePickerBinding
import java.util.Calendar

class DatePickerDialogFragment(
    private val onDateSelected: (startDate: String, endDate: String?) -> Unit,
    private val initialStartDate: String? = null,
    private val initialEndDate: String? = null
) : DialogFragment() {

    private var _binding: DialogDatePickerBinding? = null
    private val binding get() = _binding!!

    private var selectedStartDate: String? = initialStartDate
    private var selectedEndDate: String? = initialEndDate

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDatePickerBinding.inflate(layoutInflater)

        setupDatePickers()
        setupButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupDatePickers() {
        // Устанавливаем начальные значения
        binding.etStartDate.setText(selectedStartDate)
        binding.etEndDate.setText(selectedEndDate)

        // Обработчик для иконки event
        binding.ivEvent.setOnClickListener {
            Toast.makeText(requireContext(), "Open calendar", Toast.LENGTH_SHORT).show()
        }

        // Обработчик для выбора даты начала (клик по полю)
        binding.etStartDate.setOnClickListener {
            showDatePicker { date ->
                selectedStartDate = date
                binding.etStartDate.setText(date)
            }
        }

        // Обработчик для выбора даты окончания (клик по полю)
        binding.etEndDate.setOnClickListener {
            showDatePicker { date ->
                selectedEndDate = date
                binding.etEndDate.setText(date)
            }
        }
    }

    private fun showDatePicker(onDatePicked: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%02d/%02d/%d", month + 1, dayOfMonth, year)
                onDatePicked(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            val startDate = binding.etStartDate.text.toString()
            if (startDate.isBlank()) {
                Toast.makeText(requireContext(), "Please select start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val endDate = binding.etEndDate.text.toString().takeIf { it.isNotBlank() }
            onDateSelected(startDate, endDate)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}