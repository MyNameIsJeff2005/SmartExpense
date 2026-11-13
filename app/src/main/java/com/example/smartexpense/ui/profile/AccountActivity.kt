package com.example.smartexpense.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.R
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.databinding.ActivityAccountBinding
import com.example.smartexpense.ui.auth.AuthActivity
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.AvatarStore
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.viewmodel.AccountViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn Tài khoản (mục 5.8): xem/sửa ảnh đại diện, tên, giới tính và tuổi.
 *
 * - Tên ghi vào Room + Firebase Auth; accounts.csv được dựng lại để khớp dữ liệu.
 * - Giới tính / tuổi / đường dẫn ảnh lưu trong AppPrefs.
 * - Email lấy từ hồ sơ, chỉ đọc.
 */
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private val viewModel: AccountViewModel by viewModels()

    private var pendingAvatarPath: String? = null
    private var selectedGender: String = AppPrefs.GENDER_MALE
    private var profileBound = false

    /** Trang chỉ đọc khi vừa mở màn; bấm "Edit account" mới được sửa (1.3.1) */
    private var isEditing = false

    /** Photo picker của Android (không cần quyền bộ nhớ) */
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) copyAvatar(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnUpload.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.genderMale.setOnClickListener { selectGender(AppPrefs.GENDER_MALE) }
        binding.genderFemale.setOnClickListener { selectGender(AppPrefs.GENDER_FEMALE) }
        binding.btnEditAccount.setOnClickListener { applyEditMode(true) }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }

        // Mở màn ở trạng thái chỉ đọc; "Edit account" mới mở khóa các trường
        applyEditMode(false)

        // Giá trị đã lưu hiển thị ngay, không chờ đọc Room
        pendingAvatarPath = viewModel.avatarPath.value
        selectGender(viewModel.gender.value)
        val savedAge = viewModel.age.value
        binding.edtAge.setText(if (savedAge > 0) savedAge.toString() else "")
        renderAvatar(pendingAvatarPath)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profile.collect { profile ->
                    if (profile != null && !profileBound) bindProfile(profile)
                }
            }
        }
    }

    private fun bindProfile(profile: UserProfile) {
        profileBound = true
        // Không ghi đè nếu người dùng đã bắt đầu nhập tên trước khi Room tải xong
        if (binding.edtName.text.isNullOrBlank()) binding.edtName.setText(profile.displayName)
        binding.tvEmail.text = profile.email
    }

    /** Đổi trạng thái chọn của hai nút giới tính (nam/nữ). */
    private fun selectGender(gender: String) {
        selectedGender = gender
        val male = gender == AppPrefs.GENDER_MALE

        binding.genderMale.setBackgroundResource(
            if (male) R.drawable.bg_circle_selected else R.drawable.bg_circle_unselected
        )
        binding.tvGenderMale.setTextColor(getColor(if (male) R.color.white else R.color.text_secondary))

        binding.genderFemale.setBackgroundResource(
            if (male) R.drawable.bg_circle_unselected else R.drawable.bg_circle_selected
        )
        binding.tvGenderFemale.setTextColor(getColor(if (male) R.color.text_secondary else R.color.white))
    }

    /**
     * Khóa/mở khóa màn Tài khoản (1.3.1):
     * - Chỉ đọc: tên/tuổi không nhập được, không đổi giới tính/ảnh, nút ✓ ẩn.
     * - Sửa (Edit account): nhập tên/tuổi, đổi giới tính, tải ảnh, hiện ✓ để lưu.
     */
    private fun applyEditMode(editing: Boolean) {
        isEditing = editing
        binding.edtName.isEnabled = editing
        binding.edtAge.isEnabled = editing
        binding.genderMale.isClickable = editing
        binding.genderFemale.isClickable = editing
        binding.btnUpload.visibility = if (editing) View.VISIBLE else View.GONE
        binding.btnSave.visibility = if (editing) View.VISIBLE else View.GONE
        binding.btnEditAccount.visibility = if (editing) View.GONE else View.VISIBLE
        // Gợi ý thị giác: trường đang khóa hiển thị mờ hơn
        val alpha = if (editing) 1f else 0.6f
        binding.edtName.alpha = alpha
        binding.edtAge.alpha = alpha
    }

    /** Hiển thị ảnh đã lưu hoặc biểu tượng người mặc định. */
    private fun renderAvatar(path: String?) {
        val bitmap = AvatarStore.load(path)
        if (bitmap != null) {
            binding.imgAvatar.setImageBitmap(bitmap)
            binding.imgAvatar.visibility = View.VISIBLE
            binding.imgAvatarPlaceholder.visibility = View.GONE
        } else {
            binding.imgAvatar.visibility = View.GONE
            binding.imgAvatarPlaceholder.visibility = View.VISIBLE
        }
    }

    /** Sao chép ảnh người dùng chọn vào bộ nhớ app rồi xem trước. */
    private fun copyAvatar(uri: Uri) {
        val uid = viewModel.uid ?: "local"
        lifecycleScope.launch {
            val path = AvatarStore.save(this@AccountActivity, uid, uri)
            if (path == null) {
                Toast.makeText(
                    this@AccountActivity,
                    R.string.account_pick_image_failed,
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            pendingAvatarPath = path
            renderAvatar(path)
        }
    }

    private fun save() {
        val name = binding.edtName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.account_name_required, Toast.LENGTH_SHORT).show()
            return
        }

        val ageText = binding.edtAge.text.toString().trim()
        val age = if (ageText.isEmpty()) 0 else ageText.toIntOrNull() ?: -1
        if (ageText.isNotEmpty() && (age < 1 || age > 120)) {
            Toast.makeText(this, R.string.account_age_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        viewModel.save(name, selectedGender, age, pendingAvatarPath.orEmpty()) { ok ->
            if (!isFinishing && !isDestroyed) {
                if (ok) {
                    Toast.makeText(this, R.string.account_save_done, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this, R.string.account_save_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ==================== Xóa tài khoản (1.3.1) ====================

    /**
     * Hộp thoại "Are you sure you're going to delete this account?" với nút
     * **Yes** có đếm ngược 5 giây để chống bấm nhầm: nút khóa 5 giây, sau đó mở.
     */
    private fun confirmDeleteAccount() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.account_delete)
            .setMessage(R.string.account_delete_confirm_msg)
            .setNegativeButton(R.string.account_no, null)
            .setPositiveButton(R.string.account_yes, null)
            .create()

        dialog.setOnShowListener {
            val yes = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            yes.isEnabled = false
            // Nút chỉ mở sau đếm ngược; trước đó disabled nên click không vào đây
            yes.setOnClickListener { dialog.dismiss(); performDelete() }
            lifecycleScope.launch {
                for (second in DELETE_COUNTDOWN downTo 1) {
                    yes.text = getString(R.string.account_yes_countdown, second)
                    delay(1_000)
                }
                if (dialog.isShowing && !isFinishing && !isDestroyed) {
                    yes.text = getString(R.string.account_yes)
                    yes.isEnabled = true
                }
            }
        }
        dialog.show()
    }

    /** Người dùng đã xác nhận: xóa sổ tài khoản khỏi database rồi về màn Đăng nhập. */
    private fun performDelete() {
        binding.btnDeleteAccount.isEnabled = false
        binding.btnEditAccount.isEnabled = false
        viewModel.deleteAccount { ok ->
            if (isFinishing || isDestroyed) return@deleteAccount
            if (ok) {
                Toast.makeText(this, R.string.account_delete_done, Toast.LENGTH_LONG).show()
                // Phiên hết hiệu lực -> về màn Đăng nhập, dọn sạch stack
                startActivity(
                    Intent(this, AuthActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                )
                finish()
            } else {
                binding.btnDeleteAccount.isEnabled = true
                binding.btnEditAccount.isEnabled = true
                Toast.makeText(this, R.string.account_delete_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private companion object {
        /** Số giây khóa nút Yes trước khi cho phép xóa (chống bấm nhầm). */
        const val DELETE_COUNTDOWN = 5
    }
}
