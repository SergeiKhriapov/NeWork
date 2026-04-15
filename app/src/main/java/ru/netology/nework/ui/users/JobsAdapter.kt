package ru.netology.nework.ui.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.databinding.ItemJobBinding
import ru.netology.nework.model.Job
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class JobsAdapter : ListAdapter<Job, JobsAdapter.JobViewHolder>(JobDiffCallback()) {

    private var onLinkClickListener: ((String) -> Unit)? = null
    private var onJobDeleteClickListener: ((Job) -> Unit)? = null
    private var showDeleteButton = false

    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
    }

    fun setOnJobDeleteClickListener(listener: (Job) -> Unit) {
        onJobDeleteClickListener = listener
    }

    fun setShowDeleteButton(show: Boolean) {
        showDeleteButton = show
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JobViewHolder(binding, onLinkClickListener, onJobDeleteClickListener, showDeleteButton)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JobViewHolder(
        private val binding: ItemJobBinding,
        private val onLinkClickListener: ((String) -> Unit)?,
        private val onJobDeleteClickListener: ((Job) -> Unit)?,
        private val showDeleteButton: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

        fun bind(job: Job) {
            binding.apply {
                tvCompanyName.text = job.name
                tvPosition.text = job.position

                val startDate = formatDate(job.start)
                val endDate = job.finish?.let { formatDate(it) } ?: "НВ"
                tvPeriod.text = "$startDate – $endDate"

                if (!job.link.isNullOrBlank()) {
                    tvLink.text = job.link
                    llLink.visibility = android.view.View.VISIBLE
                    llLink.setOnClickListener {
                        onLinkClickListener?.invoke(job.link)
                    }
                } else {
                    llLink.visibility = android.view.View.GONE
                }

                btnDelete.visibility = if (showDeleteButton) android.view.View.VISIBLE else android.view.View.GONE
                btnDelete.setOnClickListener {
                    onJobDeleteClickListener?.invoke(job)
                }
            }
        }

        private fun formatDate(date: OffsetDateTime): String {
            return try {
                date.format(dateFormatter)
            } catch (e: Exception) {
                date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        }
    }

    class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Job, newItem: Job) = oldItem == newItem
    }
}