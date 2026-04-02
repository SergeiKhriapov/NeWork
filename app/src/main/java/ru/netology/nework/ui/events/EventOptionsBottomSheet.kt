package ru.netology.nework.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import ru.netology.nework.R
import ru.netology.nework.databinding.BottomSheetEventOptionsBinding
import ru.netology.nework.model.EventType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class EventOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEventOptionsBinding? = null
    private val binding get() = _binding!!

    private var selectedType: EventType = EventType.OFFLINE
    private var selectedDateTime: LocalDateTime = LocalDateTime.now().plusHours(1)

    var onOptionsSelected: ((eventType: EventType, dateTime: LocalDateTime) -> Unit)? = null

    fun setInitialValues(eventType: EventType, dateTime: LocalDateTime) {
        selectedType = eventType
        selectedDateTime = dateTime
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEventOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateTimePicker()
        setupTypeSelector()
        updateDateTimeDisplay()
    }

    override fun onStop() {
        super.onStop()
        // При закрытии Bottom Sheet (свайп вниз или нажатие на пустое место) сохраняем значения
        onOptionsSelected?.invoke(selectedType, selectedDateTime)
    }

    private fun setupDateTimePicker() {
        val textInputLayout = binding.etDateTime.parent.parent as TextInputLayout
        textInputLayout.setEndIconOnClickListener {
            showDateTimePicker()
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.set(selectedDateTime.year, selectedDateTime.monthValue - 1,
            selectedDateTime.dayOfMonth, selectedDateTime.hour, selectedDateTime.minute)

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                        updateDateTimeDisplay()
                        // Не закрываем, только обновляем
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeDisplay() {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm", Locale.getDefault())
        binding.etDateTime.setText(selectedDateTime.format(formatter))
    }

    private fun setupTypeSelector() {
        binding.rbOffline.isChecked = selectedType == EventType.OFFLINE
        binding.rbOnline.isChecked = selectedType == EventType.ONLINE

        binding.rbOffline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedType = EventType.OFFLINE
                // Не закрываем, только обновляем
            }
        }

        binding.rbOnline.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedType = EventType.ONLINE
                // Не закрываем, только обновляем
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): EventOptionsBottomSheet = EventOptionsBottomSheet()
    }
}