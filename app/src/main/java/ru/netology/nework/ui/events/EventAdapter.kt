package ru.netology.nework.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.ItemEventBinding
import ru.netology.nework.model.Event
import ru.netology.nework.model.EventType
import ru.netology.nework.utils.DateUtils.formatForDisplay

class EventAdapter(
    private val onLike: (Event) -> Unit,
    private val onParticipate: (Event) -> Unit,
    private val onShare: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, onLike, onParticipate, onShare)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onLike: (Event) -> Unit,
        private val onParticipate: (Event) -> Unit,
        private val onShare: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) = with(binding) {
            // Header
            tvAuthor.text = event.author
            tvDate.text = event.published.formatForDisplay()

            Glide.with(itemView)
                .load(event.authorAvatar)
                .placeholder(R.drawable.ic_account_circle)
                .circleCrop()
                .into(ivAvatar)

            // Event info
            tvEventStatus.text = when (event.type) {
                EventType.OFFLINE -> "Offline"
                EventType.ONLINE -> "Online"
            }
            tvEventDate.text = event.datetime.formatForDisplay()

            // Content
            tvContent.text = event.content

            // Link
            if (!event.link.isNullOrBlank()) {
                tvLink.visibility = View.VISIBLE
                tvLink.text = event.link
            } else {
                tvLink.visibility = View.GONE
            }

            // Media
            when (event.attachment?.type) {
                ru.netology.nework.model.AttachmentType.IMAGE -> {
                    mediaContainer.visibility = View.VISIBLE
                    ivImage.visibility = View.VISIBLE
                    Glide.with(itemView)
                        .load(event.attachment.url)
                        .centerCrop()
                        .into(ivImage)
                }
                ru.netology.nework.model.AttachmentType.VIDEO -> {
                    mediaContainer.visibility = View.VISIBLE
                    ivVideoPreview.visibility = View.VISIBLE
                    ivPlay.visibility = View.VISIBLE
                    Glide.with(itemView)
                        .load(event.attachment.url)
                        .centerCrop()
                        .frame(1000000)
                        .into(ivVideoPreview)
                }
                else -> mediaContainer.visibility = View.GONE
            }

            // Audio
            audioPlayer.visibility = if (event.attachment?.type == ru.netology.nework.model.AttachmentType.AUDIO) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Actions
            tvLikes.text = event.likeOwnerIds.size.toString()
            ivLike.setImageResource(
                if (event.likedByMe) R.drawable.ic_liked else R.drawable.ic_like
            )

            tvParticipants.text = event.participantsIds.size.toString()
            ivParticipants.setImageResource(
                if (event.participatedByMe) R.drawable.ic_mentioned else R.drawable.ic_people
            )

            // Click listeners
            btnLike.setOnClickListener { onLike(event) }
            btnParticipants.setOnClickListener { onParticipate(event) }
            btnShare.setOnClickListener { onShare(event) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}