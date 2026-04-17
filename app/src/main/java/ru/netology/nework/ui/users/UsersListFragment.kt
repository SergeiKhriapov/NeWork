package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.model.User

@AndroidEntryPoint
class UsersListFragment : Fragment() {

    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UsersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val users = arguments?.getParcelableArrayList<User>("users") ?: arrayListOf()
        val title = arguments?.getString("title") ?: "Users"

        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = title

        setupRecyclerView(users)
    }

    private fun setupRecyclerView(users: List<User>) {
        adapter = UsersAdapter { user ->
            try {
                val bundle = Bundle().apply {
                    putLong("user_id", user.id)
                    putParcelable("user", user)
                }
                findNavController().navigate(R.id.userDetailFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        adapter.submitList(users)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}