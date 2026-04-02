package ru.netology.nework.ui.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentEventDetailBinding
import ru.netology.nework.model.EventType
import ru.netology.nework.utils.DateUtils.formatForDisplay
import ru.netology.nework.utils.LetterAvatarDrawable

private const val TAG = "EventDetailFragment"

@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getLong("eventId") ?: run {
            Log.e(TAG, "No eventId in arguments")
            Toast.makeText(requireContext(), "Ошибка: событие не найдено", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        Log.d(TAG, "Loading event with id: $eventId")

        // Заглушка - показываем прогресс
        binding.progressBar.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE

        // TODO: Загрузить событие из ViewModel
        // viewModel.loadEvent(eventId)

        // Временная заглушка - показываем сообщение
        binding.progressBar.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
        binding.tvPlaceholder.text = "Детали события ${eventId}\n\nФункционал в разработке"
        binding.tvPlaceholder.visibility = View.VISIBLE

        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}