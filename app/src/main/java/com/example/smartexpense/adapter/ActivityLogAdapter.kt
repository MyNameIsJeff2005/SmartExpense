package com.example.smartexpense.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemActivityLogBinding
import com.example.smartexpense.model.AdminLogEntity
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages

/**
 * Nhật ký hoạt động admin (mục 6.7): tên hành động + uid mục tiêu + thời điểm.
 * Hành động ghi theo key ("LOCK", "REBUILD_CSV"...) -> chuỗi log_action_* theo ngôn ngữ.
 */
class ActivityLogAdapter :
    RecyclerView.Adapter<ActivityLogAdapter.Holder>() {

    private var logs: List<AdminLogEntity> = emptyList()

    fun submit(newLogs: List<AdminLogEntity>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemActivityLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = logs.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val log = logs[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.tvAction.text = context.getString(
            when (log.action) {
                Messages.LOG_LOCK -> R.string.log_action_lock
                Messages.LOG_UNLOCK -> R.string.log_action_unlock
                Messages.LOG_RESET_PW -> R.string.log_action_reset_pw
                Messages.LOG_REBUILD_CSV -> R.string.log_action_rebuild_csv
                Messages.LOG_DELETE_CSV -> R.string.log_action_delete_csv
                Messages.LOG_TOGGLE_CSV -> R.string.log_action_toggle_csv
                else -> R.string.log_action_lock
            }
        )
        binding.tvDetail.text = "uid: " + log.targetUid + " · " +
            DateTimeUtils.formatDateTime(log.timestamp)
    }

    class Holder(val binding: ItemActivityLogBinding) : RecyclerView.ViewHolder(binding.root)
}
