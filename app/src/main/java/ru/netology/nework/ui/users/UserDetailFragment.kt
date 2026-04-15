package ru.netology.nework.ui.users

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.app.MainActivity
import ru.netology.nework.databinding.FragmentUserDetailBinding
import ru.netology.nework.model.Job
import ru.netology.nework.model.Post
import ru.netology.nework.model.User
import ru.netology.nework.utils.LetterAvatarDrawable

private const val TAG = "UserDetailFragment"

@AndroidEntryPoint
class UserDetailFragment : Fragment() {

    private var _binding: FragmentUserDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserDetailViewModel by viewModels()

    private lateinit var wallPostsFragment: WallPostsFragment
    private lateinit var jobsFragment: JobsFragment
    private lateinit var pagerAdapter: UserDetailPagerAdapter

    var isMyProfile = false
        private set

    private var pendingJobs: List<Job>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getLong("user_id") ?: return
        val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("user", User::class.java)
        } else {
            arguments?.getParcelable("user")
        }
        isMyProfile = arguments?.getBoolean("is_my_profile", false) ?: false

        Log.d(TAG, "isMyProfile = $isMyProfile")

        (requireActivity() as? MainActivity)?.updateLogoutButtonVisibility(isMyProfile)

        setupFragments()
        setupTabs()
        setupListeners()

        viewModel.loadUserDetail(userId, user)
        observeViewModel()
    }

    private fun setupFragments() {
        wallPostsFragment = WallPostsFragment.newInstance()
        jobsFragment = JobsFragment.newInstance()

        pagerAdapter = UserDetailPagerAdapter(
            requireActivity(),
            wallPostsFragment,
            jobsFragment
        )
        binding.viewPager.adapter = pagerAdapter

        waitForJobsFragmentAndShowFab(0)
    }

    private fun waitForJobsFragmentAndShowFab(attempt: Int) {
        if (!isAdded || _binding == null) {
            Log.d(TAG, "Fragment not added or binding null, stopping retry")
            return
        }

        if (attempt >= 20) {
            Log.e(TAG, "Failed to initialize JobsFragment after 20 attempts")
            return
        }

        binding.viewPager.postDelayed({
            if (!isAdded || _binding == null) {
                Log.d(TAG, "Fragment not added or binding null after delay, stopping")
                return@postDelayed
            }

            val isJobsFragmentReady = try {
                jobsFragment.isAdded && jobsFragment.view != null
            } catch (e: Exception) {
                false
            }

            if (isJobsFragmentReady) {
                Log.d(TAG, "JobsFragment is ready (attempt ${attempt + 1})")

                if (isMyProfile) {
                    Log.d(TAG, "My profile - showing FAB")
                    binding.fabAddJob.visibility = View.VISIBLE
                    binding.fabAddJob.setOnClickListener {
                        showAddJobDialog()
                    }
                    jobsFragment.showDeleteButton(true)
                    jobsFragment.setOnJobDeleteClickListener { job ->
                        viewModel.deleteJob(job)
                    }
                } else {
                    Log.d(TAG, "Not my profile - hiding FAB")
                    binding.fabAddJob.visibility = View.GONE
                    jobsFragment.showDeleteButton(false)
                }

                pendingJobs?.let { jobs ->
                    jobsFragment.submitList(jobs)
                    Log.d(TAG, "Submitted pending ${jobs.size} jobs to JobsFragment")
                    pendingJobs = null
                }
            } else {
                Log.d(TAG, "JobsFragment not ready yet, retrying (attempt ${attempt + 1})")
                waitForJobsFragmentAndShowFab(attempt + 1)
            }
        }, 200)
    }

    private fun setupTabs() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Wall"
                1 -> "Jobs"
                else -> ""
            }
        }.attach()
        Log.d(TAG, "Tabs setup complete")
    }

    private fun setupListeners() {
        wallPostsFragment.setOnLikeListener { post ->
            viewModel.onLikePost(post)
        }

        wallPostsFragment.setOnShareListener { post ->
            sharePost(post)
        }

        wallPostsFragment.setOnOpenListener { post ->
            openPost(post)
        }

        jobsFragment.setOnLinkClickListener { url ->
            openLink(url)
        }
    }

    private fun observeViewModel() {
        viewModel.userDetail.observe(viewLifecycleOwner) { detail ->
            detail?.let {
                Log.d(TAG, "User detail loaded: ${it.user.name}, jobs=${it.jobs.size}")
                updateUserInfo(it.user)
                updateToolbarTitle(it.user)
                wallPostsFragment.submitList(it.wallPosts)

                if (::jobsFragment.isInitialized && jobsFragment.isAdded && jobsFragment.view != null) {
                    jobsFragment.submitList(it.jobs)
                    Log.d(TAG, "Submitted ${it.jobs.size} jobs to JobsFragment directly")
                } else {
                    Log.d(TAG, "JobsFragment not ready, saving ${it.jobs.size} jobs for later")
                    pendingJobs = it.jobs
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e(TAG, "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserInfo(user: User) {
        binding.apply {
            if (!user.avatar.isNullOrBlank()) {
                Glide.with(requireContext())
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .centerCrop()
                    .into(ivAvatar)
            } else {
                val firstLetter = user.name.firstOrNull()?.toString() ?: "?"
                val letterDrawable = LetterAvatarDrawable(
                    letter = firstLetter,
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.purple_primary)
                )
                ivAvatar.setImageDrawable(letterDrawable)
            }
        }
    }

    private fun updateToolbarTitle(user: User) {
        val title = "${user.name} / ${user.login}"
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = title
    }

    private fun sharePost(post: Post) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, post.content)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share post"))
    }

    private fun openPost(post: Post) {
        // TODO: Открыть детальный просмотр поста
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun showAddJobDialog() {
        Log.d(TAG, "showAddJobDialog() called")
        val dialog = AddJobDialog { job ->
            Log.d(TAG, "Job created: ${job.name}")
            viewModel.createJob(job)
        }
        dialog.show(childFragmentManager, "AddJobDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: Long, user: User? = null, isMyProfile: Boolean = false): UserDetailFragment {
            return UserDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong("user_id", userId)
                    if (user != null) {
                        putParcelable("user", user)
                    }
                    putBoolean("is_my_profile", isMyProfile)
                }
            }
        }
    }
}