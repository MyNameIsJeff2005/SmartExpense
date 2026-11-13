package com.example.smartexpense.ui.admin
import com.example.smartexpense.adapter.UserAdapter

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.databinding.FragmentUserListBinding
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

/**
 * Tab Người dùng (mục 6.3): tìm theo tên/email (2 cột lớn không phân trang -
 * nhóm 1.248 giai đoạn V1), click -> Chi tiết người dùng (6.4).
 */
class UserListFragment : Fragment() {

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()

    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserAdapter { user ->
            startActivity(
                Intent(requireContext(), UserDetailActivity::class.java)
                    .putExtra(UserDetailActivity.EXTRA_UID, user.uid)
            )
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        binding.edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.setQuery(s?.toString() ?: "")
            }
        })

        viewModel.refreshTotal()
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.users.collect { users ->
                        adapter.submit(users)
                        binding.tvEmpty.visibility =
                            if (users.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.totalUsers.collect {
                        binding.tvTotal.text = CurrencyUtils.formatCount(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTotal()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
