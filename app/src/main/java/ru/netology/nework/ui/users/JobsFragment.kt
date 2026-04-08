package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.model.Job

class JobsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: JobsAdapter
    private var onLinkClickListener: ((String) -> Unit)? = null

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
    }

    private fun setupRecyclerView() {
        adapter = JobsAdapter()
        adapter.setOnLinkClickListener { url ->
            onLinkClickListener?.invoke(url)
        }
        binding.rvJobs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobs.adapter = adapter
    }

    fun submitList(jobs: List<Job>) {
        if (::adapter.isInitialized) {
            adapter.submitList(jobs)
        }
    }

    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
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