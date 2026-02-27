package ru.netology.nework.ui.feed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
    private var previousPostsSize = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_posts)

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

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            Log.d("FeedFragment", "Received ${posts.size} posts")
            if (posts.isNotEmpty()) {
                val firstFew = posts.take(5).joinToString { "${it.id} (${it.published})" }
                Log.d("FeedFragment", "First 5 with dates: $firstFew")
            }

            val isNewPostAdded = posts.size > previousPostsSize
            adapter.submitList(posts)

            if (isNewPostAdded) {
                recyclerView.post { recyclerView.scrollToPosition(0) }
            }

            previousPostsSize = posts.size
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.authError.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() {
        super.onResume()

        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)

        fab.show()
        fab.setOnClickListener {
            if (viewModel.isLoggedIn()) {
                findNavController().navigate(R.id.newPostFragment)
            } else {
                Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<FloatingActionButton>(R.id.fab_create).hide()
    }

    private fun showPopupMenu(post: Post, anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_post_actions, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    val bundle = bundleOf(
                        "postId" to post.id,
                        "content" to post.content,
                        "attachmentUrl" to post.attachment?.url,
                        "attachmentType" to post.attachment?.type?.name
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
        val shareText = "${post.author}: ${post.content}\n\nПрочитайте в приложении NeWork"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться постом"))
    }
}