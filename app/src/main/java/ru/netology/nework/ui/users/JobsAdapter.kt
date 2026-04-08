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

    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JobViewHolder(binding, onLinkClickListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JobViewHolder(
        private val binding: ItemJobBinding,
        private val onLinkClickListener: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

        fun bind(job: Job) {
            binding.apply {
                tvCompanyName.text = job.name
                tvPosition.text = job.position

                // Форматирование периода работы
                val startDate = formatDate(job.start)
                val endDate = job.finish?.let { formatDate(it) } ?: "НВ"
                tvPeriod.text = "$startDate – $endDate"

                // Обработка ссылки
                if (!job.link.isNullOrBlank()) {
                    tvLink.text = job.link
                    llLink.visibility = android.view.View.VISIBLE
                    llLink.setOnClickListener {
                        onLinkClickListener?.invoke(job.link)
                    }
                } else {
                    llLink.visibility = android.view.View.GONE
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