package com.example.smartexpense.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.AdminLogEntity
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CategoryRepository
import com.example.smartexpense.repository.CsvRepository
import com.example.smartexpense.repository.TransactionRepository
import com.example.smartexpense.repository.UserSyncRepository
import com.example.smartexpense.ui.common.BarChartView
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/**
 * ViewModel cho toàn bộ phần quản trị (mục 6):
 * tổng quan (số liệu thật), user (tìm kiếm/khóa), CSV (trạng thái/rebuild/xóa/công tắc),
 * danh mục (thêm/sửa) và nhật ký hoạt động. Admin không xem nội dung giao dịch của user khác.
 */
class AdminViewModel(private val app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository(app)
    val prefs = com.example.smartexpense.utils.AppPrefs.get(app)

    private val db get() = AppDatabase.get(app)
    private val txRepo = TransactionRepository(app)

    // ==================== Tab Người dùng ====================

    val searchQuery = MutableStateFlow("")

    private val userDao get() = db.userProfileDao()

    val totalUsers = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val users: StateFlow<List<UserProfile>> = searchQuery.flatMapLatest { q ->
        userDao.observeAll().map { list ->
            val term = q.trim().lowercase()
            if (term.isEmpty()) list
            else list.filter { it.displayName.lowercase().contains(term) || it.email.lowercase().contains(term) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) {
        searchQuery.value = query
    }

    fun refreshTotal() {
        viewModelScope.launch { totalUsers.value = withContext(Dispatchers.IO) { userDao.countProfiles() } }
    }

    // ==================== Tab Tổng quan ====================

    data class DashboardData(
        val userCount: Int = 0,
        val txCount: Int = 0,
        val activeToday: Int = 0,
        val lockedCount: Int = 0,
        val weekCounts: List<Int> = emptyList(),
        val growthPercent: Int? = null
    )

    private val _dashboard = MutableStateFlow(DashboardData())
    val dashboard: StateFlow<DashboardData> = _dashboard

    fun refreshDashboard() {
        viewModelScope.launch {
            _dashboard.value = withContext(Dispatchers.IO) {
                val today = DateTimeUtils.today()
                val dayStart = DateTimeUtils.startOfDay(today)
                val dayEnd = DateTimeUtils.endOfDay(today)
                // Biểu đồ 7 ngày gần nhất (T2->CN theo 4.2): từng ngày có bao nhiêu hồ sơ tạo?
                val weekCounts = (6 downTo 0).map { offset ->
                    val day = today.minusDays(offset.toLong())
                    userDao.countCreatedBetween(DateTimeUtils.startOfDay(day), DateTimeUtils.endOfDay(day))
                }
                val currentWeek = userDao.countCreatedBetween(
                    DateTimeUtils.startOfDay(today.minusDays(6L)), dayEnd
                )
                val previousWeek = userDao.countCreatedBetween(
                    DateTimeUtils.startOfDay(today.minusDays(13L)),
                    DateTimeUtils.endOfDay(today.minusDays(7L))
                )
                val growth = if (previousWeek > 0) {
                    ((currentWeek - previousWeek) * 100.0 / previousWeek).let { it.toInt() }
                } else {
                    null
                }
                DashboardData(
                    userCount = userDao.countProfiles(),
                    txCount = txRepo.countAll(),
                    activeToday = userDao.countActiveBetween(dayStart, dayEnd),
                    lockedCount = userDao.countLocked(),
                    weekCounts = weekCounts,
                    growthPercent = growth
                )
            }
        }
    }

    // ==================== Chi tiết người dùng (6.4) ====================

    private val _selectedUser = MutableStateFlow<UserProfile?>(null)
    val selectedUser: StateFlow<UserProfile?> = _selectedUser

    val selectedUserTxCount = MutableStateFlow(0)

    fun loadUser(uid: String) {
        viewModelScope.launch {
            _selectedUser.value = withContext(Dispatchers.IO) { userDao.getByUid(uid) }
            selectedUserTxCount.value = withContext(Dispatchers.IO) { txRepo.countByUid(uid) }
        }
    }

    private val currentUserUid: String get() = auth.currentUserUid() ?: ""

    /**
     * Khóa/mở khóa (mục 6.4). Chỉ đổi cờ cục bộ trong Room — không vô hiệu tài khoản
     * trên Firebase (cần Admin SDK/backend). Không khóa chính mình hoặc admin khác.
     */
    fun toggleLock(target: UserProfile) {
        if (target.role == UserProfile.ROLE_ADMIN || target.uid == currentUserUid) return
        viewModelScope.launch {
            val locked = target.status == UserProfile.STATUS_LOCKED
            withContext(Dispatchers.IO) {
                db.userProfileDao().let { dao ->
                    if (locked) dao.unlockUser(target.uid) else dao.lockUser(target.uid)
                    db.adminLogDao().insert(
                        AdminLogEntity(
                            action = if (locked) Messages.LOG_UNLOCK else Messages.LOG_LOCK,
                            targetUid = target.uid,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            refreshDashboard(); loadUser(target.uid)
        }
    }

    /** "Đặt lại mật khẩu": gửi email reset - admin không bao giờ biết/đặt hộ mật khẩu */
    val resetState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    fun resetPassword(email: String) {
        resetState.value = UiState.Loading
        viewModelScope.launch {
            auth.sendPasswordReset(email).fold(
                onSuccess = { resetState.value = UiState.Success(Unit) },
                onFailure = { resetState.value = UiState.Error(app.getString(Messages.RESET_FAILED)) }
            )
        }
    }

    // ==================== CSV (mục 3.2c) ====================

    val csvStatus = MutableStateFlow<CsvRepository.CsvStatus?>(null)

    fun refreshCsv() {
        viewModelScope.launch { csvStatus.value = CsvRepository.status(getApplication()) }
    }

    fun rebuildCsv() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { CsvRepository.rebuildFromRoom(getApplication()) }
            db.adminLogDao().insert(
                AdminLogEntity(action = Messages.LOG_REBUILD_CSV, targetUid = currentUserUid, timestamp = System.currentTimeMillis())
            )
            refreshCsv(); refreshDashboard()
        }
    }

    fun deleteCsv() {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { CsvRepository.delete(getApplication()) }
            if (ok) {
                db.adminLogDao().insert(
                    AdminLogEntity(action = Messages.LOG_DELETE_CSV, targetUid = currentUserUid, timestamp = System.currentTimeMillis())
                )
            }
            refreshCsv(); refreshDashboard()
        }
    }

    fun setAutoRecreate(on: Boolean) {
        prefs.autoRecreateCsv = on
        viewModelScope.launch {
            db.adminLogDao().insert(
                AdminLogEntity(action = Messages.LOG_TOGGLE_CSV + "|" + if (on) "ON" else "OFF",
                    targetUid = currentUserUid,
                    timestamp = System.currentTimeMillis())
            )
        }
        refreshCsv()
    }

    // ==================== Xuất / nhập CSV qua SAF (mục 3.2c) ====================

    /** Kết quả một lần xuất/nhập CSV để Activity hiển thị Snackbar. */
    sealed interface CsvEvent {
        data class Imported(val inserted: Int, val updated: Int, val skipped: Int) : CsvEvent
        data class Exported(val rows: Int) : CsvEvent
        data class Failed(val message: String) : CsvEvent
    }

    private val _csvEvent = MutableStateFlow<CsvEvent?>(null)
    val csvEvent: StateFlow<CsvEvent?> = _csvEvent

    fun consumeCsvEvent() {
        _csvEvent.value = null
    }

    /** Xuất toàn bộ hồ sơ ra file CSV tại vị trí người dùng chọn. */
    fun exportCsvTo(uri: Uri) {
        viewModelScope.launch {
            runCatching { CsvRepository.exportTo(getApplication(), uri) }
                .onSuccess { rows ->
                    _csvEvent.value = CsvEvent.Exported(rows)
                    logAction(Messages.LOG_EXPORT_CSV)
                }
                .onFailure { _csvEvent.value = CsvEvent.Failed(it.message.orEmpty()) }
            refreshCsv(); refreshDashboard()
        }
    }

    /** Nhập CSV từ file người dùng chọn, gộp vào Room rồi dựng lại accounts.csv. */
    fun importCsvFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching { CsvRepository.importFrom(getApplication(), uri) }
                .onSuccess { result ->
                    _csvEvent.value = CsvEvent.Imported(result.inserted, result.updated, result.skipped)
                    logAction(Messages.LOG_IMPORT_CSV)
                }
                .onFailure { _csvEvent.value = CsvEvent.Failed(it.message.orEmpty()) }
            refreshCsv(); refreshDashboard(); refreshTotal()
        }
    }

    // ==================== Đồng bộ Firebase Firestore (mục 3.2) ====================

    private val syncRepo = UserSyncRepository(app)

    /** Tên cơ sở dữ liệu đám mây đang dùng, hiển thị ở tab Cài đặt admin. */
    val apiBaseUrl = MutableStateFlow(syncRepo.baseUrl())

    /** Trạng thái đồng bộ: Success(message) / Error(message) / Loading. */
    val syncState = MutableStateFlow<UiState<String>>(UiState.Idle)

    fun pullFromServer() {
        if (syncState.value is UiState.Loading) return
        viewModelScope.launch {
            syncState.value = UiState.Loading
            syncRepo.pullAll().fold(
                onSuccess = { s ->
                    logAction(Messages.LOG_SYNC_PULL)
                    syncState.value = UiState.Success(app.getString(Messages.SYNC_PULL_DONE, s.pulled))
                },
                onFailure = { syncState.value = UiState.Error(it.message.orEmpty()) }
            )
            refreshCsv(); refreshDashboard(); refreshTotal(); refreshApiBase()
        }
    }

    fun pushToServer() {
        if (syncState.value is UiState.Loading) return
        viewModelScope.launch {
            syncState.value = UiState.Loading
            syncRepo.pushAll().fold(
                onSuccess = { s ->
                    logAction(Messages.LOG_SYNC_PUSH)
                    syncState.value = UiState.Success(app.getString(Messages.SYNC_PUSH_DONE, s.pushed))
                },
                onFailure = { syncState.value = UiState.Error(it.message.orEmpty()) }
            )
            refreshApiBase()
        }
    }

    fun refreshApiBase() {
        apiBaseUrl.value = syncRepo.baseUrl()
    }

    private suspend fun logAction(action: String) {
        runCatching {
            db.adminLogDao().insert(
                AdminLogEntity(
                    action = action,
                    targetUid = currentUserUid,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // ==================== Danh mục (6.6) ====================

    private val categoryRepo = CategoryRepository(app)

    val categories: StateFlow<List<CategoryEntity>> = categoryRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insertCategory(name: String, type: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepo.insert(name, type, colorHex)
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String, newType: String, newColorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepo.update(category.copy(name = newName, type = newType, colorHex = newColorHex))
        }
    }

    fun signOut() {
        AuthRepository(app).signOut()
    }

    // ==================== Nhật ký hoạt động (6.7) ====================

    val logs: StateFlow<List<AdminLogEntity>> = db.adminLogDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
