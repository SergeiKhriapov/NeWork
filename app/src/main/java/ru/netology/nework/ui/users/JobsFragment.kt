package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.model.Job

class JobsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JobsAdapter
    private var onLinkClickListener: ((String) -> Unit)? = null
    private var onJobDeleteClickListener: ((Job) -> Unit)? = null

    private val viewModel: UserDetailViewModel by activityViewModels()
    private var showDelete = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = JobsAdapter()

        adapter.setShowDeleteButton(showDelete)

        adapter.setOnJobDeleteClickListener { job ->
            onJobDeleteClickListener?.invoke(job)
        }

        adapter.setOnLinkClickListener { url ->
            onLinkClickListener?.invoke(url)
        }

        binding.rvJobs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobs.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.userDetail.observe(viewLifecycleOwner) { detail ->
            detail?.let {
                adapter.submitList(it.jobs)
            }
        }
    }

    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
        if (::adapter.isInitialized) {
            adapter.setOnLinkClickListener(listener)
        }
    }

    fun setOnJobDeleteClickListener(listener: (Job) -> Unit) {
        onJobDeleteClickListener = listener
        if (::adapter.isInitialized) {
            adapter.setOnJobDeleteClickListener(listener)
        }
    }

    fun showDeleteButton(show: Boolean) {
        showDelete = show
        if (::adapter.isInitialized) {
            adapter.setShowDeleteButton(show)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): JobsFragment {
            return JobsFragment()
        }
    }
}