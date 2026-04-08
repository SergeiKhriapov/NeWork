package ru.netology.nework.ui.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.netology.nework.databinding.FragmentWallPostsBinding
import ru.netology.nework.model.Post
import ru.netology.nework.ui.feed.PostAdapter

class WallPostsFragment : Fragment() {

    private var _binding: FragmentWallPostsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter
    private var onLikeListener: ((Post) -> Unit)? = null
    private var onOpenListener: ((Post) -> Unit)? = null
    private var onShareListener: ((Post) -> Unit)? = null
    private var onMenuListener: ((Post, View) -> Unit)? = null
    private var onPlayMediaListener: ((String, Boolean) -> Unit)? = null
    private var isOwnedByUser: ((Post) -> Boolean)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWallPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(
            onLike = { post -> onLikeListener?.invoke(post) ?: Unit },
            onOpen = { post -> onOpenListener?.invoke(post) ?: Unit },
            onMenu = { post, view -> onMenuListener?.invoke(post, view) ?: Unit },
            onPlayMedia = { url, isVideo -> onPlayMediaListener?.invoke(url, isVideo) ?: Unit },
            onShare = { post -> onShareListener?.invoke(post) ?: Unit },
            isOwnedByUser = { post -> isOwnedByUser?.invoke(post) ?: false }
        )

        binding.rvWallPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWallPosts.adapter = adapter
    }

    fun submitList(posts: List<Post>) {
        if (::adapter.isInitialized) {
            adapter.submitList(posts)
        }
    }

    fun setOnLikeListener(listener: (Post) -> Unit) {
        onLikeListener = listener
    }

    fun setOnOpenListener(listener: (Post) -> Unit) {
        onOpenListener = listener
    }

    fun setOnShareListener(listener: (Post) -> Unit) {
        onShareListener = listener
    }

    fun setOnMenuListener(listener: (Post, View) -> Unit) {
        onMenuListener = listener
    }

    fun setOnPlayMediaListener(listener: (String, Boolean) -> Unit) {
        onPlayMediaListener = listener
    }

    fun setIsOwnedByUser(listener: (Post) -> Boolean) {
        isOwnedByUser = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): WallPostsFragment {
            return WallPostsFragment()
        }
    }
}