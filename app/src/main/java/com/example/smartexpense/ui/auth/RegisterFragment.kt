package com.example.smartexpense.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.R
import com.example.smartexpense.databinding.FragmentRegisterBinding
import com.example.smartexpense.ui.main.MainActivity
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.viewmodel.AuthViewModel
import com.example.smartexpense.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Tab 2/3 của AuthActivity: đăng ký (mục 5.1).
 * Thành công -> vào MainActivity; nếu ghi CSV thất bại (csvOk=false) hiện cảnh báo trung thực.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val fullName = binding.edtFullName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString()
            val confirm = binding.edtConfirmPassword.text.toString()

            var valid = true
            if (fullName.isEmpty()) {
                binding.inputFullName.error = getString(R.string.error_required); valid = false
            } else {
                binding.inputFullName.error = null
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.inputEmail.error = getString(R.string.error_invalid_email); valid = false
            } else {
                binding.inputEmail.error = null
            }
            if (password.length < 6) {
                binding.inputPassword.error = getString(R.string.error_password_weak); valid = false
            } else {
                binding.inputPassword.error = null
            }
            if (password != confirm) {
                binding.inputConfirmPassword.error = getString(R.string.error_password_mismatch); valid = false
            } else {
                binding.inputConfirmPassword.error = null
            }
            if (!valid) return@setOnClickListener

            viewModel.register(fullName, email, password)
        }

        binding.btnGoLogin.setOnClickListener {
            (requireActivity() as AuthActivity).navigateTo(LoginFragment())
        }
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: UiState<com.example.smartexpense.repository.AuthRepository.AuthOutcome>) {
        val currentBinding = _binding ?: return
        currentBinding.btnRegister.isEnabled = state !is UiState.Loading
        when (state) {
            is UiState.Loading -> currentBinding.btnRegister.text = getString(R.string.btn_register) + "..."
            is UiState.Error -> {
                currentBinding.btnRegister.text = getString(R.string.btn_register)
                Snackbar.make(currentBinding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.clearStates()
            }
            is UiState.Success -> {
                currentBinding.btnRegister.text = getString(R.string.btn_register)
                val outcome = state.data ?: return
                viewModel.clearStates()
                if (!outcome.csvOk) {
                    // Không giấu lỗi CSV: báo rõ, vẫn cho vào app (Room đã ghi hồ sơ)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.app_name))
                        .setMessage(getString(Messages.CSV_SAVE_FAILED))
                        .setPositiveButton(getString(R.string.btn_ok), null)
                        .show()
                }
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            UiState.Idle -> {
                currentBinding.btnRegister.text = getString(R.string.btn_register)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
