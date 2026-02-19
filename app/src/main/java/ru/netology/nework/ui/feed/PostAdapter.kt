package ru.netology.nework.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.ItemPostBinding
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Post
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val onLike: (Post) -> Unit,
    private val onOpen: (Post) -> Unit,
    private val onMenu: (Post, View) -> Unit,
    private val onPlayVideo: (String) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val b: ItemPostBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(post: Post) = with(b) {

            // ---------- TEXT ----------
            tvAuthor.text = post.author

            tvDate.text = SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(post.published))

            if (post.content.isBlank()) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = post.content
            }

            tvLikes.text = post.likes.toString()

            // ---------- LIKE ----------
            ivLike.setImageResource(
                if (post.likedByMe)
                    R.drawable.ic_liked
                else
                    R.drawable.ic_like
            )

            btnLike.setOnClickListener { onLike(post) }

            // ---------- RESET MEDIA ----------
            mediaContainer.visibility = View.GONE
            ivImage.visibility = View.GONE
            ivVideoPreview.visibility = View.GONE
            ivPlay?.visibility = View.GONE

            Glide.with(itemView).clear(ivImage)
            ivVideoPreview.let { Glide.with(itemView).clear(it) }

            // ---------- ATTACHMENT ----------
            val attachment = post.attachment
            if (attachment != null) {

                mediaContainer.visibility = View.VISIBLE

                when (attachment.type) {

                    AttachmentType.IMAGE -> {
                        ivImage.visibility = View.VISIBLE

                        Glide.with(itemView)
                            .load(attachment.url)
                            .centerCrop()
                            .into(ivImage)

                        ivImage.setOnClickListener {
                            onOpen(post)
                        }
                    }

                    AttachmentType.VIDEO -> {

                        val previewUrl = attachment.url
                        val videoUrl = post.link

                        ivVideoPreview.visibility = View.VISIBLE
                        ivPlay?.visibility = View.VISIBLE

                        ivVideoPreview.let {
                            Glide.with(itemView)
                                .load(previewUrl)
                                .centerCrop()
                                .into(it)
                        }

                        if (!videoUrl.isNullOrBlank()) {
                            ivVideoPreview.setOnClickListener {
                                onPlayVideo(videoUrl)
                            }

                            ivPlay?.setOnClickListener {
                                onPlayVideo(videoUrl)
                            }
                        }
                    }


                    else -> {
                        mediaContainer.visibility = View.GONE
                    }
                }
            }

            // ---------- OTHER CLICKS ----------
            root.setOnClickListener { onOpen(post) }
            btnMore.setOnClickListener { onMenu(post, btnMore) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Post, newItem: Post) =
            oldItem == newItem
    }
}
