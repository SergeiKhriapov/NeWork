package ru.netology.nework.ui.users

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.model.Job

private const val TAG = "JobsFragment"

class JobsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: JobsAdapter
    private var onLinkClickListener: ((String) -> Unit)? = null
    private var onJobDeleteClickListener: ((Job) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = JobsAdapter()
        adapter.setOnLinkClickListener { url ->
            Log.d(TAG, "Link clicked: $url")
            onLinkClickListener?.invoke(url)
        }
        adapter.setOnJobDeleteClickListener { job ->
            Log.d(TAG, "Delete job: ${job.name}")
            onJobDeleteClickListener?.invoke(job)
        }
        binding.rvJobs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJobs.adapter = adapter
        Log.d(TAG, "RecyclerView setup complete")
    }

    fun submitList(jobs: List<Job>) {
        if (::adapter.isInitialized) {
            val oldSize = adapter.currentList.size
            Log.d(TAG, "submitList: ${jobs.size} jobs (old size: $oldSize)")
            adapter.submitList(jobs)

            // Если добавилась новая работа (размер увеличился), скроллим вверх
            if (jobs.size > oldSize) {
                binding.rvJobs.postDelayed({
                    binding.rvJobs.smoothScrollToPosition(0)
                    Log.d(TAG, "Scrolled to top after adding new job")
                }, 100)
            }
        } else {
            Log.e(TAG, "Adapter not initialized, jobs will be submitted later")
        }
    }

    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
    }

    fun setOnJobDeleteClickListener(listener: (Job) -> Unit) {
        onJobDeleteClickListener = listener
    }

    fun showDeleteButton(show: Boolean) {
        if (::adapter.isInitialized) {
            adapter.setShowDeleteButton(show)
            adapter.notifyDataSetChanged()
            Log.d(TAG, "showDeleteButton called with show = $show")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }

    companion object {
        fun newInstance(): JobsFragment {
            return JobsFragment()
        }
    }
}