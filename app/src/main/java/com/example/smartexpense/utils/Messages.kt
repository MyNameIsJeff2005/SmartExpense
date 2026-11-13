package com.example.smartexpense.utils

import androidx.annotation.StringRes
import com.example.smartexpense.R

/**
 * Các thông báo sinh ra lúc chạy (mục 3.1.11 - mọi lỗi đều thân thiện, không crash).
 *
 * Từ bản 1.1, mọi chuỗi hiển thị cho người dùng đều nằm trong strings.xml
 * (values = tiếng Việt, values-en = tiếng Anh), ở đây chỉ giữ ID tài nguyên
 * để code gọi `context.getString(Messages.XXX)` và Android tự chọn ngôn ngữ.
 *
 * Các hằng LOG_* là mã ghi vào nhật ký (không hiển thị trực tiếp) nên vẫn là chuỗi cố định.
 */
object Messages {

    // ---------- Auth ----------
    @StringRes val REGISTER_SUCCESS = R.string.msg_register_success
    @StringRes val LOGIN_FAILED = R.string.msg_login_failed
    @StringRes val REGISTER_FAILED = R.string.msg_register_failed
    @StringRes val EMAIL_EXISTS = R.string.msg_email_exists
    @StringRes val WEAK_PASSWORD = R.string.msg_weak_password
    @StringRes val WRONG_CREDENTIALS = R.string.msg_wrong_credentials
    @StringRes val AUTH_FAILED = R.string.msg_auth_failed
    @StringRes val NOT_CONFIGURED = R.string.msg_not_configured
    @StringRes val ACCOUNT_LOCKED = R.string.msg_account_locked
    @StringRes val RESET_SENT = R.string.msg_reset_sent
    @StringRes val RESET_FAILED = R.string.msg_reset_failed

    // ---------- CSV ----------
    @StringRes val CSV_SAVE_FAILED = R.string.msg_csv_save_failed
    @StringRes val CSV_SHARED = R.string.msg_csv_shared
    @StringRes val CSV_SHARE_FAILED = R.string.msg_csv_share_failed
    @StringRes val CSV_REBUILT = R.string.msg_csv_rebuilt
    @StringRes val CSV_DELETED = R.string.msg_csv_deleted
    @StringRes val CSV_DELETE_FAILED = R.string.msg_csv_delete_failed
    @StringRes val CSV_IMPORT_FAILED = R.string.msg_csv_import_failed
    @StringRes val CSV_IMPORT_EMPTY = R.string.msg_csv_import_empty

    // ---------- Firebase / đồng bộ hồ sơ ----------
    @StringRes val API_UNREACHABLE = R.string.msg_api_unreachable
    @StringRes val API_SYNC_FAILED = R.string.msg_api_sync_failed
    @StringRes val SYNC_PULL_DONE = R.string.msg_sync_pull_done
    @StringRes val SYNC_PUSH_DONE = R.string.msg_sync_push_done

    // ---------- Giao dịch ----------
    @StringRes val TX_SAVED = R.string.msg_tx_saved
    @StringRes val TX_DELETE_SUCCESS = R.string.msg_tx_delete_success
    @StringRes val TX_LOAD_FAILED = R.string.msg_tx_load_failed

    // ---------- Admin ----------
    @StringRes val LOCKED_TOAST = R.string.msg_locked_toast
    @StringRes val UNLOCKED_TOAST = R.string.msg_unlocked_toast
    @StringRes val RESET_SENT_TO = R.string.msg_reset_sent_to
    @StringRes val ADMIN_NOT_FOUND = R.string.msg_admin_not_found

    // ---------- Ghi nhật ký hành động (mã, không hiển thị) ----------
    const val LOG_LOCK = "LOCK"
    const val LOG_UNLOCK = "UNLOCK"
    const val LOG_RESET_PW = "RESET_PW"
    const val LOG_REBUILD_CSV = "REBUILD_CSV"
    const val LOG_DELETE_CSV = "DELETE_CSV"
    const val LOG_TOGGLE_CSV = "TOGGLE_CSV"
    const val LOG_IMPORT_CSV = "IMPORT_CSV"
    const val LOG_EXPORT_CSV = "EXPORT_CSV"
    const val LOG_SYNC_PULL = "SYNC_PULL"
    const val LOG_SYNC_PUSH = "SYNC_PUSH"
}
