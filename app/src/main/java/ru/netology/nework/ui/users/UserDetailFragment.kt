package ru.netology.nework.ui.users

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentUserDetailBinding
import ru.netology.nework.model.Post
import ru.netology.nework.model.User
import ru.netology.nework.utils.LetterAvatarDrawable

@AndroidEntryPoint
class UserDetailFragment : Fragment() {

    private var _binding: FragmentUserDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserDetailViewModel by viewModels()

    private lateinit var wallPostsFragment: WallPostsFragment
    private lateinit var jobsFragment: JobsFragment
    private lateinit var pagerAdapter: UserDetailPagerAdapter

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

        // Получаем аргументы из Bundle
        val userId = arguments?.getLong("user_id") ?: return
        val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("user", User::class.java)
        } else {
            arguments?.getParcelable("user")
        }

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
    }

    private fun setupTabs() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Wall"
                1 -> "Jobs"
                else -> ""
            }
        }.attach()
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
                updateUserInfo(it.user)
                updateToolbarTitle(it.user)
                wallPostsFragment.submitList(it.wallPosts)
                jobsFragment.submitList(it.jobs)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
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
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, post.content)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share post"))
    }

    private fun openPost(post: Post) {
        // Открытие детального просмотра поста
    }

    private fun openLink(url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: Long, user: User? = null): UserDetailFragment {
            return UserDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong("user_id", userId)
                    if (user != null) {
                        putParcelable("user", user)
                    }
                }
            }
        }
    }
}