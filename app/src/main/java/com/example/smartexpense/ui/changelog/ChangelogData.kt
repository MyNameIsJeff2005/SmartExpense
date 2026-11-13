package com.example.smartexpense.ui.changelog

import androidx.annotation.StringRes
import com.example.smartexpense.BuildConfig
import com.example.smartexpense.R

/** Loại thay đổi để tô màu nhãn: thêm mới / sửa / xóa. */
enum class ChangeKind { ADDED, CHANGED, REMOVED }

/** Một dòng thay đổi trong nhật ký. */
data class Change(
    @StringRes val text: Int,
    val kind: ChangeKind
)

/** Một phiên bản trong nhật ký thay đổi. */
data class VersionEntry(
    val version: String,
    val date: String,
    val isLatest: Boolean = false,
    val changes: List<Change>
)

/**
 * Nội dung Nhật ký thay đổi (mục 5.8 - Giới thiệu).
 *
 * Nội dung được viết song ngữ qua string resource nên tự đổi theo ngôn ngữ app.
 * Danh sách sắp xếp mới nhất trước; phiên bản mới nhất lấy trực tiếp từ
 * [BuildConfig.VERSION_NAME] để không phải sửa hai nơi khi tăng phiên bản.
 */
object ChangelogData {

    val versions: List<VersionEntry> = listOf(
        VersionEntry(
            version = BuildConfig.VERSION_NAME,
            date = "12/11/2026",
            isLatest = true,
            changes = listOf(
                Change(R.string.cl_140_add_exchange_rate, ChangeKind.ADDED),
                Change(R.string.cl_140_add_package_structure, ChangeKind.ADDED),
                Change(R.string.cl_140_update_room_v2, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.3.1",
            date = "03/11/2026",
            changes = listOf(
                Change(R.string.cl_1311_add_account_edit_delete, ChangeKind.ADDED),
                Change(R.string.cl_1311_add_chart_anim, ChangeKind.ADDED),
                Change(R.string.cl_1311_fix_calendar_dark, ChangeKind.CHANGED),
                Change(R.string.cl_1311_fix_calendar_empty, ChangeKind.REMOVED),
                Change(R.string.cl_1311_fix_chart_button, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.3.0",
            date = "02/11/2026",
            changes = listOf(
                Change(R.string.cl_1300_add_firestore, ChangeKind.ADDED),
                Change(R.string.cl_1300_add_restore, ChangeKind.ADDED),
                Change(R.string.cl_1300_add_sync, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.2.4",
            date = "31/10/2026",
            changes = listOf(
                Change(R.string.cl_124_add_changelog_ui, ChangeKind.ADDED),
                Change(R.string.cl_124_add_chart_toggle, ChangeKind.ADDED),
                Change(R.string.cl_124_add_bar_chart, ChangeKind.ADDED),
                Change(R.string.cl_124_fix_pie_full, ChangeKind.CHANGED),
                Change(R.string.cl_124_fix_about_row, ChangeKind.CHANGED),
                Change(R.string.cl_124_remove_old_changelog, ChangeKind.REMOVED)
            )
        ),
        VersionEntry(
            version = "1.2.3",
            date = "30/10/2026",
            changes = listOf(
                Change(R.string.cl_130_add_changelog, ChangeKind.ADDED),
                Change(R.string.cl_130_fix_backup_path, ChangeKind.CHANGED),
                Change(R.string.cl_130_fix_backup_error, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.2.2",
            date = "30/10/2026",
            changes = listOf(
                Change(R.string.cl_122_add_backup_restore, ChangeKind.ADDED),
                Change(R.string.cl_122_add_auto_backup, ChangeKind.ADDED),
                Change(R.string.cl_122_add_backup_path, ChangeKind.ADDED),
                Change(R.string.cl_122_change_file_name, ChangeKind.CHANGED),
                Change(R.string.cl_122_remove_csv_rows, ChangeKind.REMOVED)
            )
        ),
        VersionEntry(
            version = "1.2.1",
            date = "30/10/2026",
            changes = listOf(
                Change(R.string.cl_121_add_settings_tab, ChangeKind.ADDED),
                Change(R.string.cl_121_add_account_screen, ChangeKind.ADDED),
                Change(R.string.cl_121_add_home_logo, ChangeKind.ADDED),
                Change(R.string.cl_121_fix_insets, ChangeKind.CHANGED),
                Change(R.string.cl_121_fix_fab, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.2.0",
            date = "29/10/2026",
            changes = listOf(
                Change(R.string.cl_120_add_category_manage, ChangeKind.ADDED),
                Change(R.string.cl_120_add_activity_log, ChangeKind.ADDED),
                Change(R.string.cl_120_add_sample_data, ChangeKind.ADDED),
                Change(R.string.cl_120_change_stats, ChangeKind.CHANGED),
                Change(R.string.cl_120_fix_sync, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.1.0",
            date = "20/10/2026",
            changes = listOf(
                Change(R.string.cl_110_add_auth, ChangeKind.ADDED),
                Change(R.string.cl_110_add_admin_users, ChangeKind.ADDED),
                Change(R.string.cl_110_add_search_filter, ChangeKind.ADDED),
                Change(R.string.cl_110_add_budget, ChangeKind.ADDED),
                Change(R.string.cl_110_fix_currency, ChangeKind.CHANGED)
            )
        ),
        VersionEntry(
            version = "1.0.0",
            date = "10/10/2026",
            changes = listOf(
                Change(R.string.cl_100_add_app, ChangeKind.ADDED),
                Change(R.string.cl_100_add_transactions, ChangeKind.ADDED),
                Change(R.string.cl_100_add_overview, ChangeKind.ADDED),
                Change(R.string.cl_100_add_list_calendar, ChangeKind.ADDED),
                Change(R.string.cl_100_add_stats, ChangeKind.ADDED),
                Change(R.string.cl_100_add_theme_language, ChangeKind.ADDED),
                Change(R.string.cl_100_add_room_offline, ChangeKind.ADDED)
            )
        )
    )
}
