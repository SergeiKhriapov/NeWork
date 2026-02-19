package ru.netology.nework.ui.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ru.netology.nework.R
import ru.netology.nework.model.Post
import ru.netology.nework.viewmodel.FeedViewModel

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_posts)

        adapter = PostAdapter(
            onLike = { post ->
                viewModel.toggleLike(post.id)
            },
            onOpen = { post ->
                findNavController().navigate(
                    R.id.action_feed_to_postDetail,
                    bundleOf("postId" to post.id)
                )
            },
            onMenu = { post, anchor ->
                showPopupMenu(post, anchor)
            },
            onPlayVideo = { url ->   // ✅ добавили
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        )


        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }
    }

    override fun onResume() {
        super.onResume()

        val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab_create)

        fab.show()
        fab.setOnClickListener {
            if (viewModel.isLoggedIn()) {
                findNavController().navigate(R.id.action_feed_to_postCreate)
            } else {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity()
            .findViewById<FloatingActionButton>(R.id.fab_create)
            .hide()
    }

    private fun showPopupMenu(post: Post, anchor: View) {
        // TODO: реализовать PopupMenu
    }
}
