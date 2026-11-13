package com.example.smartexpense.utils

import android.content.Context
import com.example.smartexpense.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Hộp thoại chọn chế độ sáng/tối, dùng chung cho màn Cá nhân và màn Cài đặt admin.
 * Chỉ một nơi định nghĩa để hai màn hình luôn có cùng lựa chọn và cùng nhãn.
 */
object ThemeDialog {

    fun show(
        context: Context,
        currentMode: String,
        onPicked: (String) -> Unit
    ) {
        val labels = ThemeManager.labels(context)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(labels, ThemeManager.indexOf(currentMode)) { dialog, which ->
                dialog.dismiss()
                onPicked(ThemeManager.MODES[which])
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
