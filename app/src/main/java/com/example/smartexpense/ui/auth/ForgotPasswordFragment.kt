package com.example.smartexpense.ui.auth

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
import com.example.smartexpense.databinding.FragmentForgotBinding
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.viewmodel.AuthViewModel
import com.example.smartexpense.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Tab 3/3 của AuthActivity: quên mật khẩu (mục 3.4).
 * Firebase gửi email đặt lại không cần đăng nhập.
 */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendReset.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.inputEmail.error = getString(R.string.error_invalid_email)
                return@setOnClickListener
            }
            binding.inputEmail.error = null
            viewModel.sendPasswordReset(email)
        }

        binding.btnBackLogin.setOnClickListener {
            (requireActivity() as AuthActivity).navigateTo(LoginFragment())
        }
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.forgotState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: UiState<Unit>) {
        val currentBinding = _binding ?: return
        currentBinding.btnSendReset.isEnabled = state !is UiState.Loading
        when (state) {
            is UiState.Loading -> currentBinding.btnSendReset.text = getString(R.string.btn_send_reset) + "..."
            is UiState.Error -> {
                currentBinding.btnSendReset.text = getString(R.string.btn_send_reset)
                Snackbar.make(currentBinding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.clearStates()
            }
            is UiState.Success -> {
                currentBinding.btnSendReset.text = getString(R.string.btn_send_reset)
                viewModel.clearStates()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.auth_forgot_title))
                    .setMessage(getString(Messages.RESET_SENT))
                    .setPositiveButton(getString(R.string.btn_ok), null)
                    .show()
            }
            UiState.Idle -> {
                currentBinding.btnSendReset.text = getString(R.string.btn_send_reset)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
