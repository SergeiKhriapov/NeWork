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
import androidx.navigation.fragment.findNavController
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

    override fun onResume() {
        super.onResume()
        showFab()
        viewModel.loadEvents()
    }

    override fun onPause() {
        super.onPause()
        hideFab()
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

    private fun setupFab() {
        try {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)
            fab?.show()
            fab?.setOnClickListener {
                findNavController().navigate(R.id.newEventFragment)
            }
        } catch (e: Exception) {
        }
    }

    private fun showFab() {
        try {
            requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)?.show()
        } catch (e: Exception) {
        }
    }

    private fun hideFab() {
        try {
            requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)?.hide()
        } catch (e: Exception) {
        }
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

    private fun openEventDetails(event: Event) {
        findNavController().navigate(
            R.id.action_events_to_eventDetail,
            bundleOf("eventId" to event.id)
        )
    }

    private fun showPopupMenu(event: Event, anchor: View) {
        try {
            val popup = android.widget.PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.menu_post_actions, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        editEvent(event)
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
        } catch (e: Exception) {
        }
    }

    private fun editEvent(event: Event) {
        val bundle = Bundle().apply {
            putLong("eventId", event.id)
            putString("content", event.content)
            putString("eventType", event.type.name)
            putString("eventDateTime", event.datetime.toString())
            putString("attachmentUrl", event.attachment?.url ?: "")
            putString("attachmentType", event.attachment?.type?.name ?: "")
            putDouble("lat", event.coords?.lat ?: 0.0)
            putDouble("lng", event.coords?.lng ?: 0.0)
            putLongArray("participantIds", event.participantsIds.toLongArray())
        }

        findNavController().navigate(R.id.newEventFragment, bundle)
    }

    private fun showDeleteConfirmation(eventId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteEvent(eventId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun shareEvent(event: Event) {
        val shareText = "${event.author}: ${event.content}\n\n" +
                "📅 ${event.datetime.formatForDisplay()}\n" +
                "📍 ${event.type}\n\n" +
                "Join on NeWork!"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "Share event"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}