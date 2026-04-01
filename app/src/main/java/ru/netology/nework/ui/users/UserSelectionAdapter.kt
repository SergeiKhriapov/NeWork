package ru.netology.nework.ui.users

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.ItemUserCheckableBinding
import ru.netology.nework.model.User
import ru.netology.nework.utils.LetterAvatarDrawable

private const val TAG = "UserSelectionAdapter"

class UserSelectionAdapter(
    private val onItemClick: (User, Boolean) -> Unit
) : ListAdapter<User, UserSelectionAdapter.UserViewHolder>(DiffCallback) {

    private val selectedIds = mutableSetOf<Long>()

    fun getSelectedIds(): Set<Long> = selectedIds.toSet()

    fun setSelectedIds(ids: Set<Long>) {
        Log.d(TAG, "setSelectedIds: $ids")
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserCheckableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserCheckableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentUser: User? = null

        init {
            binding.cbSelected.setOnCheckedChangeListener { _, isChecked ->
                currentUser?.let { user ->
                    if (selectedIds.contains(user.id) != isChecked) {
                        if (isChecked) {
                            selectedIds.add(user.id)
                        } else {
                            selectedIds.remove(user.id)
                        }
                        // Уведомляем об изменении при каждом клике
                        onItemClick(user, isChecked)
                        Log.d(TAG, "Selection changed: user=${user.id}, isChecked=$isChecked, selectedIds=$selectedIds")
                    }
                }
            }
        }

        fun bind(user: User) {
            currentUser = user
            binding.apply {
                tvName.text = user.name
                tvLogin.text = user.login

                if (!user.avatar.isNullOrBlank()) {
                    Glide.with(itemView)
                        .load(user.avatar)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .circleCrop()
                        .into(ivAvatar)
                } else {
                    val firstLetter = user.name.firstOrNull()?.toString() ?: "?"
                    val drawable = LetterAvatarDrawable(
                        letter = firstLetter,
                        backgroundColor = ContextCompat.getColor(itemView.context, R.color.purple_primary)
                    )
                    ivAvatar.setImageDrawable(drawable)
                }

                // Временно отключаем слушатель, чтобы не вызвать событие при установке состояния
                cbSelected.setOnCheckedChangeListener(null)
                cbSelected.isChecked = selectedIds.contains(user.id)
                // Возвращаем слушатель
                cbSelected.setOnCheckedChangeListener { _, isChecked ->
                    currentUser?.let { u ->
                        if (selectedIds.contains(u.id) != isChecked) {
                            if (isChecked) {
                                selectedIds.add(u.id)
                            } else {
                                selectedIds.remove(u.id)
                            }
                            onItemClick(u, isChecked)
                        }
                    }
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}