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
import com.example.smartexpense.databinding.FragmentLoginBinding
import com.example.smartexpense.ui.main.MainActivity
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.viewmodel.AuthViewModel
import com.example.smartexpense.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Tab 1/3 của AuthActivity: đăng nhập (mục 5.1).
 * Validate cục bộ trước, gọi AuthViewModel; thành công -> MainActivity (clearTask).
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString()

            var valid = true
            // Kiểm tra bắt buộc + định dạng email (mục 3.1)
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.inputEmail.error = getString(R.string.error_invalid_email)
                valid = false
            } else {
                binding.inputEmail.error = null
            }
            if (password.isEmpty()) {
                binding.inputPassword.error = getString(R.string.error_required)
                valid = false
            } else {
                binding.inputPassword.error = null
            }
            if (!valid) return@setOnClickListener

            viewModel.login(email, password)
        }

        binding.btnForgot.setOnClickListener {
            (requireActivity() as AuthActivity).navigateTo(ForgotPasswordFragment())
        }

        binding.btnGoRegister.setOnClickListener {
            (requireActivity() as AuthActivity).navigateTo(RegisterFragment())
        }

        binding.btnDemo.setOnClickListener {
            viewModel.demo()
        }
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.loginState.collect { state -> render(state) } }
                launch { viewModel.demoState.collect { state -> render(state) } }
            }
        }
    }

    private fun render(state: UiState<Unit>) {
        val currentBinding = _binding ?: return // view có thể đã hủy khi activity đang finish
        currentBinding.btnLogin.isEnabled = state !is UiState.Loading
        when (state) {
            is UiState.Loading -> {
                currentBinding.btnLogin.text = getString(R.string.btn_login) + "..."
            }
            is UiState.Error -> {
                currentBinding.btnLogin.text = getString(R.string.btn_login)
                if (state.message == getString(Messages.ACCOUNT_LOCKED)) {
                    // Tài khoản bị khóa: hộp thoại nhấn mạnh (mục 3.1.2)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.status_locked))
                        .setMessage(state.message)
                        .setPositiveButton(getString(R.string.btn_ok), null)
                        .show()
                } else {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                viewModel.clearStates()
            }
            is UiState.Success -> {
                // Đăng nhập OK: vào màn chính, xóa back stack auth (mục 2)
                viewModel.clearStates()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            UiState.Idle -> {
                currentBinding.btnLogin.text = getString(R.string.btn_login)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
