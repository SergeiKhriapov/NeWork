package ru.netology.nework.ui.feed

import android.util.Log
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

private const val TAG = "PostAdapter"

class PostAdapter(
    private val onLike: (Post) -> Unit,
    private val onOpen: (Post) -> Unit,
    private val onMenu: (Post, View) -> Unit,
    private val onPlayMedia: (String, Boolean) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.d(TAG, "onCreateViewHolder")
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder position=$position")
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val b: ItemPostBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(post: Post) = with(b) {
            Log.d(TAG, "bind post id=${post.id}, type=${post.attachment?.type}")

            // ---------- AUTHOR ----------
            tvAuthor.text = post.author

            // ---------- AVATAR ----------
            if (!post.authorAvatar.isNullOrBlank()) {
                Glide.with(itemView)
                    .load(post.authorAvatar)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_account_circle)
            }

            // ---------- DATE ----------
            tvDate.text = SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            ).format(Date(post.published))

            // ---------- CONTENT ----------
            if (post.content.isBlank()) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.visibility = View.VISIBLE
                tvContent.text = post.content
            }

            // ---------- LIKES ----------
            tvLikes.text = post.likes.toString()
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
                        Log.d(TAG, "Post ${post.id}: IMAGE attachment")
                        ivImage.visibility = View.VISIBLE
                        Glide.with(itemView)
                            .load(attachment.url)
                            .centerCrop()
                            .into(ivImage)
                        ivImage.setOnClickListener { onOpen(post) }
                    }

                    AttachmentType.VIDEO -> {
                        Log.d(TAG, "Post ${post.id}: VIDEO attachment")
                        // Определяем URL видео (для воспроизведения)
                        val videoUrl = attachment.url
                        // Определяем источник превью:
                        // 1) Если post.link не пуст и отличается от videoUrl, используем его как превью (старый формат)
                        // 2) Иначе пытаемся извлечь кадр из самого видео через Glide
                        if (!post.link.isNullOrBlank() && post.link != videoUrl) {
                            // Старый формат: превью в post.link
                            ivVideoPreview.visibility = View.VISIBLE
                            ivPlay?.visibility = View.VISIBLE
                            Glide.with(itemView)
                                .load(post.link)
                                .centerCrop()
                                .into(ivVideoPreview)
                        } else {
                            // Новый формат: извлекаем кадр из видео
                            ivVideoPreview.visibility = View.VISIBLE
                            ivPlay?.visibility = View.VISIBLE
                            Glide.with(itemView)
                                .load(videoUrl)
                                .frame(1000000) // кадр на 1 секунде
                                .centerCrop()
                                .placeholder(R.drawable.ic_play_circle_filled)
                                .error(R.drawable.ic_play_circle_filled)
                                .into(ivVideoPreview)
                        }

                        if (!videoUrl.isNullOrBlank()) {
                            ivVideoPreview.setOnClickListener {
                                Log.d(TAG, "Video preview clicked")
                                onPlayMedia(videoUrl, true)
                            }
                            ivPlay?.setOnClickListener {
                                Log.d(TAG, "Play button clicked")
                                onPlayMedia(videoUrl, true)
                            }
                        } else {
                            Log.e(TAG, "Video URL is null or blank for post ${post.id}")
                        }
                    }

                    AttachmentType.AUDIO -> {
                        Log.d(TAG, "Post ${post.id}: AUDIO attachment")
                        ivImage.visibility = View.VISIBLE
                        ivImage.setImageResource(R.drawable.ic_audio)
                        ivImage.setOnClickListener {
                            Log.d(TAG, "Audio icon clicked")
                            onPlayMedia(attachment.url, false)
                        }
                        ivVideoPreview.visibility = View.GONE
                        ivPlay?.visibility = View.GONE
                    }

                    else -> {
                        Log.w(TAG, "Unknown attachment type: ${attachment.type}")
                        mediaContainer.visibility = View.GONE
                    }
                }
            } else {
                Log.d(TAG, "Post ${post.id}: no attachment")
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