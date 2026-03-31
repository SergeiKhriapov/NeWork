package ru.netology.nework.ui.feed

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        Log.d("FeedFragment", "onViewCreated START")

        recyclerView = view.findViewById(R.id.rv_posts)
        Log.d("FeedFragment", "RecyclerView found: ${recyclerView != null}")

        if (recyclerView == null) {
            Log.e("FeedFragment", "RecyclerView is NULL! Check layout fragment_feed.xml")
            Toast.makeText(requireContext(), "Ошибка: RecyclerView не найден", Toast.LENGTH_LONG).show()
            return
        }

        debugTextView = TextView(requireContext()).apply {
            text = "Загрузка постов..."
            textSize = 16f
            setPadding(16, 16, 16, 16)
            visibility = View.VISIBLE
        }
        (view as? android.view.ViewGroup)?.addView(debugTextView)

        adapter = PostAdapter(
            onLike = { post -> viewModel.toggleLike(post.id) },
            onOpen = { post ->
                Log.d("FeedFragment", "Opening post: ${post.id}")
                findNavController().navigate(
                    R.id.action_feed_to_postDetail,
                    bundleOf("postId" to post.id)
                )
            },
            onMenu = { post, anchor -> showPopupMenu(post, anchor) },
            onPlayMedia = { url, isVideo ->
                Log.d("FeedFragment", "Playing media: $url, isVideo=$isVideo")
                val dialog = MediaPlayerDialog.newInstance(url, isVideo)
                dialog.show(parentFragmentManager, "media_player")
            },
            onShare = { post -> sharePost(post) },
            isOwnedByUser = { post -> post.authorId == viewModel.currentUserId.value }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Бесконечная прокрутка
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = adapter.itemCount

                if (!isLoadingMore && lastVisibleItemPosition >= totalItemCount - 3) {
                    isLoadingMore = true
                    viewModel.loadMorePosts()
                }
            }
        })

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            Log.d("FeedFragment", "Received ${posts.size} posts")

            debugTextView?.text = "Загружено постов: ${posts.size}"

            if (posts.isNotEmpty()) {
                Log.d("FeedFragment", "First post: ${posts[0].content.take(50)}")
                debugTextView?.visibility = View.GONE
            } else {
                Log.d("FeedFragment", "Posts list is empty")
                debugTextView?.text = "Нет постов. Загрузка..."
                debugTextView?.visibility = View.VISIBLE
            }

            val isNewPostAdded = posts.size > previousPostsSize
            adapter.submitList(posts)

            if (isNewPostAdded) {
                recyclerView.post {
                    recyclerView.scrollToPosition(0)
                    Log.d("FeedFragment", "Scrolled to top")
                }
            }

            previousPostsSize = posts.size
            isLoadingMore = false
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("FeedFragment", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.authError.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Log.e("FeedFragment", "Auth error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("FeedFragment", "isLoading: $isLoading")
            if (isLoading) {
                debugTextView?.text = "Загрузка постов..."
                debugTextView?.visibility = View.VISIBLE
            }
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { loading ->
            isLoadingMore = loading
            if (loading) {
                debugTextView?.text = "Загрузка ещё..."
                debugTextView?.visibility = View.VISIBLE
            } else if (adapter.itemCount > 0) {
                debugTextView?.visibility = View.GONE
            }
        }

        Log.d("FeedFragment", "onViewCreated END")
    }

    override fun onResume() {
        super.onResume()
        Log.d("FeedFragment", "onResume")

        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)
        Log.d("FeedFragment", "FAB found: ${fab != null}")

        if (fab != null) {
            fab.show()
            fab.setOnClickListener {
                Log.d("FeedFragment", "FAB clicked, isLoggedIn: ${viewModel.isLoggedIn()}")
                if (viewModel.isLoggedIn()) {
                    findNavController().navigate(R.id.newPostFragment)
                } else {
                    Toast.makeText(requireContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e("FeedFragment", "FAB is NULL!")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("FeedFragment", "onPause")
        try {
            requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)?.hide()
        } catch (e: Exception) {
            Log.e("FeedFragment", "Error hiding FAB: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("FeedFragment", "onDestroyView")
        debugTextView = null
    }

    private fun showPopupMenu(post: Post, anchor: View) {
        Log.d("FeedFragment", "Showing popup menu for post: ${post.id}")
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_post_actions, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    Log.d("FeedFragment", "Edit post: ${post.id}")
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
                    Log.d("FeedFragment", "Delete post: ${post.id}")
                    showDeleteConfirmation(post.id)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(postId: Long) {
        Log.d("FeedFragment", "Showing delete confirmation for post: $postId")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { _, _ ->
                Log.d("FeedFragment", "Confirmed delete for post: $postId")
                viewModel.deletePost(postId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun sharePost(post: Post) {
        Log.d("FeedFragment", "Sharing post: ${post.id}")
        val shareText = "${post.author}: ${post.content}\n\nПрочитайте в приложении NeWork"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться постом"))
    }
}