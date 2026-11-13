package com.example.smartexpense.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Android 15+ luôn vẽ edge-to-edge nên nội dung dễ bị đè lên thanh trạng thái
 * (taskbar) hoặc thanh điều hướng. Hàm này tắt chế độ "fit system windows" mặc định
 * rồi tự cộng padding theo kích thước thanh hệ thống.
 *
 * @param topView view cần chừa khoảng cho thanh trạng thái (thường là root hoặc vùng nội dung)
 * @param bottomView view cần chừa khoảng cho thanh điều hướng / bàn phím
 */
fun AppCompatActivity.applySystemBarInsets(
    root: View,
    topView: View = root,
    bottomView: View = root
) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        topView.updatePadding(top = bars.top)
        bottomView.updatePadding(bottom = maxOf(bars.bottom, ime.bottom))
        insets
    }
}
