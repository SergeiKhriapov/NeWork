package ru.netology.nework.ui.users

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.netology.nework.R
import ru.netology.nework.databinding.DialogAddJobBinding
import ru.netology.nework.model.Job
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class AddJobDialog(
    private val onJobCreated: (Job) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddJobBinding? = null
    private val binding get() = _binding!!

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddJobBinding.inflate(layoutInflater)

        setupDatePicker()
        setupButtons()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupDatePicker() {
        binding.tvStartDate.setOnClickListener {
            val dialog = DatePickerDialogFragment(
                onDateSelected = { startDate, endDate ->
                    selectedStartDate = startDate
                    selectedEndDate = endDate
                    val dateText = if (endDate != null) {
                        "$startDate – $endDate"
                    } else {
                        "$startDate – НВ"
                    }
                    binding.tvStartDate.text = dateText
                }
            )
            dialog.show(childFragmentManager, "DateRangePicker")
        }
    }

    private fun setupButtons() {
        binding.btnCreate.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val position = binding.etPosition.text.toString().trim()
            val link = binding.etLink.text.toString().trim().takeIf { it.isNotEmpty() }

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название компании", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (position.isEmpty()) {
                Toast.makeText(requireContext(), "Введите должность", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedStartDate == null) {
                Toast.makeText(requireContext(), "Выберите дату начала", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val job = Job(
                id = 0,
                name = name,
                position = position,
                start = parseDate(selectedStartDate!!),
                finish = selectedEndDate?.let { parseDate(it) },
                link = link
            )

            onJobCreated(job)
            dismiss()
        }
    }

    private fun parseDate(dateString: String): OffsetDateTime {
        return try {
            val localDate = java.time.LocalDate.parse(dateString, dateFormatter)
            localDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
        } catch (e: Exception) {
            OffsetDateTime.now()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}