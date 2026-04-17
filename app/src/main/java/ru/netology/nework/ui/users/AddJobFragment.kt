package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.databinding.FragmentAddJobBinding
import ru.netology.nework.model.Job
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddJobFragment : Fragment() {

    private var _binding: FragmentAddJobBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserDetailViewModel by activityViewModels()
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddJobBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDatePicker()
        setupButtons()
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
                        "$startDate – Present"
                    }
                    binding.tvStartDate.text = dateText
                },
                initialStartDate = selectedStartDate,
                initialEndDate = selectedEndDate
            )
            dialog.show(childFragmentManager, "DateRangePicker")
        }
    }

    private fun setupButtons() {
        binding.btnCreate.setOnClickListener {
            val name = binding.etCompany.text.toString().trim()
            val position = binding.etPosition.text.toString().trim()
            val link = binding.etLink.text.toString().trim().takeIf { it.isNotEmpty() }

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter company name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (position.isEmpty()) {
                Toast.makeText(requireContext(), "Enter position", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedStartDate == null) {
                Toast.makeText(requireContext(), "Select start date", Toast.LENGTH_SHORT).show()
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

            viewModel.createJob(job)
            Toast.makeText(requireContext(), "Job added", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
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