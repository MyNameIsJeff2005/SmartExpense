package com.example.smartexpense.utils

import android.content.Context
import com.example.smartexpense.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Hộp thoại chọn ngôn ngữ (Theo hệ thống / Tiếng Việt / English),
 * dùng chung cho màn Cá nhân và màn Cài đặt admin - một chỗ duy nhất
 * để hai màn hình luôn có cùng lựa chọn và cùng nhãn.
 */
object LanguageDialog {

    fun show(
        context: Context,
        onPicked: (String) -> Unit
    ) {
        val labels = LanguageManager.labels(context)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(labels, LanguageManager.indexOf(LanguageManager.current())) { dialog, which ->
                dialog.dismiss()
                onPicked(LanguageManager.LANGS[which])
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
