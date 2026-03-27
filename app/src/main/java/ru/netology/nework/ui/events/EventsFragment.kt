package ru.netology.nework.ui.events

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.model.Event
import ru.netology.nework.ui.dialog.MediaPlayerDialog
import ru.netology.nework.utils.DateUtils.formatForDisplay

@AndroidEntryPoint
class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventsViewModel by viewModels()
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter(
            onLike = { event -> viewModel.likeEvent(event.id, event.likedByMe) },
            onParticipate = { event -> viewModel.participateEvent(event.id, event.participatedByMe) },
            onOpen = { event -> openEventDetails(event) },
            onMenu = { event, anchor -> showPopupMenu(event, anchor) },
            onPlayMedia = { url, isVideo ->
                val dialog = MediaPlayerDialog.newInstance(url, isVideo)
                dialog.show(parentFragmentManager, "media_player")
            },
            onShare = { event -> shareEvent(event) },
            isOwnedByUser = { event -> event.authorId == viewModel.currentUserId.value }
        )

        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFab() {
        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)
        fab.show()
        fab.setOnClickListener {
            // TODO: Navigate to create event fragment
            Toast.makeText(requireContext(), "Создание события", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEventDetails(event: Event) {
        // TODO: Implement navigation to event details fragment
        Toast.makeText(requireContext(), "Открыть событие: ${event.id}", Toast.LENGTH_SHORT).show()
    }

    private fun showPopupMenu(event: Event, anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_post_actions, popup.menu) // Используем то же меню, что и для постов
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    // TODO: Navigate to edit event
                    Toast.makeText(requireContext(), "Редактировать событие", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation(event.id)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(eventId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Удалить событие")
            .setMessage("Вы уверены, что хотите удалить это событие?")
            .setPositiveButton("Да") { _, _ ->
                /*viewModel.deleteEvent(eventId)*/
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun shareEvent(event: Event) {
        val shareText = "${event.author}: ${event.content}\n\n" +
                "📅 ${event.datetime.formatForDisplay()}\n" +
                "📍 ${event.type}\n\n" +
                "Присоединяйтесь в NeWork!"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться событием"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Добавьте вспомогательную функцию для форматирования даты, если её нет
fun String.formatForDisplay(): String {
    // Используйте вашу существующую логику форматирования
    return this // Заглушка, замените на реальную
}