package ru.netology.nework.ui.users

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.netology.nework.R
import ru.netology.nework.app.MainActivity
import ru.netology.nework.databinding.FragmentJobsBinding
import ru.netology.nework.model.Job

private const val TAG = "JobsFragment"

class JobsFragment : Fragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: JobsAdapter
    private var onLinkClickListener: ((String) -> Unit)? = null
    private var onAddJobClickListener: (() -> Unit)? = null
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
        setupFab()
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

    private fun setupFab() {
        binding.fabAddJob.setOnClickListener {
            Log.d(TAG, "Local FAB clicked")
            onAddJobClickListener?.invoke()
        }

        // Программно устанавливаем позицию FAB в правом нижнем углу экрана
        binding.fabAddJob.post {
            val params = binding.fabAddJob.layoutParams as? FrameLayout.LayoutParams
            val density = resources.displayMetrics.density

            // Получаем высоту BottomNavigationView если она есть
            val bottomNavHeight = try {
                (requireActivity() as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottom_nav)?.height ?: 0
            } catch (e: Exception) {
                0
            }

            // Рассчитываем отступ снизу (16dp + высота BottomNav если она видна)
            val marginBottom = (160 * density).toInt() + bottomNavHeight

            params?.apply {
                gravity = Gravity.BOTTOM or Gravity.END
                bottomMargin = marginBottom
                rightMargin = (16 * density).toInt()
                leftMargin = 0
                topMargin = 0
            }
            binding.fabAddJob.layoutParams = params
        }

        Log.d(TAG, "FAB setup complete")
    }

    fun submitList(jobs: List<Job>) {
        if (::adapter.isInitialized) {
            Log.d(TAG, "submitList: ${jobs.size} jobs")
            adapter.submitList(jobs)
        } else {
            Log.e(TAG, "Adapter not initialized, jobs will be submitted later")
        }
    }

    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
    }

    fun setOnAddJobClickListener(listener: () -> Unit) {
        onAddJobClickListener = listener
    }

    fun setOnJobDeleteClickListener(listener: (Job) -> Unit) {
        onJobDeleteClickListener = listener
    }

    fun showAddButton(show: Boolean) {
        if (!isAdded || _binding == null) {
            Log.d(TAG, "Fragment not attached, skipping showAddButton")
            return
        }
        Log.d(TAG, "showAddButton called with show = $show")
        binding.fabAddJob.visibility = if (show) View.VISIBLE else View.GONE
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