package com.example.smartexpense.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemUserBinding
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.utils.UiUtils

/**
 * Dòng người dùng trong tab admin (mục 6.3): avatar chữ cái, tên, email,
 * pill trạng thái Hoạt động / Bị khóa. Click -> Chi tiết người dùng (6.4).
 */
class UserAdapter(
    private val onUserClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserAdapter.Holder>() {

    private var users: List<UserProfile> = emptyList()

    fun submit(newUsers: List<UserProfile>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val user = users[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.tvAvatar.text = UiUtils.initial(user.displayName)
        binding.tvName.text = user.displayName
        binding.tvEmail.text = user.email

        val locked = user.status == UserProfile.STATUS_LOCKED
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.setText(if (locked) R.string.status_locked else R.string.status_active)
        binding.tvStatus.setBackgroundResource(
            if (locked) R.drawable.bg_pill_red else R.drawable.bg_pill_green
        )
        binding.tvStatus.setTextColor(
            context.getColor(if (locked) R.color.red else R.color.green)
        )

        binding.root.setOnClickListener { onUserClick(user) }
    }

    class Holder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)
}
