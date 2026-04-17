package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentUserSelectionBinding
import ru.netology.nework.viewmodel.UsersViewModel

const val REQUEST_KEY = "user_selection"
const val SELECTED_USERS_KEY = "selected_users"

@AndroidEntryPoint
class UserSelectionFragment : Fragment() {

    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UsersViewModel by viewModels()
    private lateinit var adapter: UserSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("title", "Choose users") ?: "Choose users"
        (requireActivity() as AppCompatActivity).supportActionBar?.title = title

        setupRecyclerView()
        observeViewModel()

        val savedSelectedIds = viewModel.getSelectedUserIds()
        if (savedSelectedIds.isNotEmpty()) {
            adapter.setSelectedIds(savedSelectedIds)
        } else {
            arguments?.getLongArray("selected_ids")?.let { ids ->
                adapter.setSelectedIds(ids.toSet())
                viewModel.updateSelectedUsers(ids.toSet())
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_post -> {
                        confirmSelection()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        val savedSelectedIds = viewModel.getSelectedUserIds()
        if (savedSelectedIds.isNotEmpty() && ::adapter.isInitialized) {
            adapter.setSelectedIds(savedSelectedIds)
        }
    }

    private fun confirmSelection() {
        val selectedIds = adapter.getSelectedIds().toList().toLongArray()
        val bundle = Bundle().apply {
            putLongArray(SELECTED_USERS_KEY, selectedIds)
        }
        setFragmentResult(REQUEST_KEY, bundle)
        findNavController().navigateUp()
    }

    private fun setupRecyclerView() {
        adapter = UserSelectionAdapter { user, isChecked ->
            val currentIds = viewModel.getSelectedUserIds().toMutableSet()
            if (isChecked) {
                currentIds.add(user.id)
            } else {
                currentIds.remove(user.id)
            }
            viewModel.updateSelectedUsers(currentIds)
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}