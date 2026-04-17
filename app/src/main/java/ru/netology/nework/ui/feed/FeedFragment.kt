package ru.netology.nework.ui.feed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.model.Post
import ru.netology.nework.ui.dialog.MediaPlayerDialog
import ru.netology.nework.viewmodel.FeedViewModel

@AndroidEntryPoint
class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private var previousPostsSize = 0
    private var debugTextView: TextView? = null
    private var isLoadingMore = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_posts)

        if (recyclerView == null) {
            Toast.makeText(requireContext(), "Error: RecyclerView not found", Toast.LENGTH_LONG).show()
            return
        }

        debugTextView = TextView(requireContext()).apply {
            text = "Loading posts..."
            textSize = 16f
            setPadding(16, 16, 16, 16)
            visibility = View.VISIBLE
        }
        (view as? android.view.ViewGroup)?.addView(debugTextView)

        adapter = PostAdapter(
            onLike = { post -> viewModel.toggleLike(post.id) },
            onOpen = { post ->
                findNavController().navigate(
                    R.id.action_feed_to_postDetail,
                    bundleOf("postId" to post.id)
                )
            },
            onMenu = { post, anchor -> showPopupMenu(post, anchor) },
            onPlayMedia = { url, isVideo ->
                val dialog = MediaPlayerDialog.newInstance(url, isVideo)
                dialog.show(parentFragmentManager, "media_player")
            },
            onShare = { post -> sharePost(post) },
            isOwnedByUser = { post -> post.authorId == viewModel.currentUserId.value }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = adapter.itemCount

                if (!isLoadingMore && lastVisibleItemPosition >= totalItemCount - 2 && totalItemCount > 0) {
                    isLoadingMore = true
                    viewModel.loadMorePosts()
                }
            }
        })

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            debugTextView?.text = "Posts loaded: ${posts.size}"

            if (posts.isNotEmpty()) {
                debugTextView?.visibility = View.GONE
            } else {
                debugTextView?.text = "No posts. Loading..."
                debugTextView?.visibility = View.VISIBLE
            }

            val isNewPostAdded = posts.size > previousPostsSize
            adapter.submitList(posts)

            if (isNewPostAdded) {
                recyclerView.post {
                    recyclerView.scrollToPosition(0)
                }
            }

            previousPostsSize = posts.size
            isLoadingMore = false
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.authError.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                debugTextView?.text = "Loading posts..."
                debugTextView?.visibility = View.VISIBLE
            }
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { loading ->
            isLoadingMore = loading
            if (loading) {
                debugTextView?.text = "Loading more..."
                debugTextView?.visibility = View.VISIBLE
            } else if (adapter.itemCount > 0) {
                debugTextView?.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)

            fab?.let {
                it.show()
                it.setOnClickListener {
                    if (viewModel.isLoggedIn()) {
                        findNavController().navigate(R.id.newPostFragment)
                    } else {
                        Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)?.hide()
        } catch (e: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debugTextView = null
    }

    private fun showPopupMenu(post: Post, anchor: View) {
        try {
            val popup = android.widget.PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.menu_post_actions, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        val bundle = bundleOf(
                            "postId" to post.id,
                            "content" to post.content,
                            "attachmentUrl" to (post.attachment?.url ?: ""),
                            "attachmentType" to (post.attachment?.type?.name ?: ""),
                            "lat" to (post.coords?.lat ?: 0.0),
                            "lng" to (post.coords?.lng ?: 0.0),
                            "mentionIds" to post.mentionIds.toLongArray()
                        )
                        findNavController().navigate(R.id.newPostFragment, bundle)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmation(post.id)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        } catch (e: Exception) {
        }
    }

    private fun showDeleteConfirmation(postId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deletePost(postId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun sharePost(post: Post) {
        val shareText = "${post.author}: ${post.content}\n\nRead in NeWork app"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "Share post"))
    }
}