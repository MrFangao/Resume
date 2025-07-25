package com.example.drawingapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.example.drawingapp.DrawingViewModel
import com.example.drawingapp.MainActivity
import com.example.drawingapp.databinding.FragmentLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.drawingapp.R

class loginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化 Firebase Auth
        auth = Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.registerButton.setOnClickListener {
            handleRegister()
        }
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            // 验证输入
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "邮箱和密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // 显示加载状态
            binding.progressBar.visibility = View.VISIBLE

            // 执行登录
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        // 登录成功
                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        // 导航到绘画界面
                        navigateToDrawFragment()
                    } else {
                        // 登录失败
                        Toast.makeText(context, "登录失败: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
    private fun handleRegister() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        // 验证输入
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "邮箱和密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证密码长度
        if (password.length < 6) {
            Toast.makeText(context, "密码长度必须大于6位", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证邮箱格式
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "请输入有效的邮箱地址", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示加载状态
        binding.progressBar.visibility = View.VISIBLE

        // 执行注册
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    // 注册成功
                    Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
                    // 直接导航到绘画界面
                    navigateToDrawFragment()
                } else {
                    // 注册失败
                    when (task.exception?.message) {
                        "The email address is already in use by another account." ->
                            Toast.makeText(context, "该邮箱已被注册", Toast.LENGTH_SHORT).show()
                        "The email address is badly formatted." ->
                            Toast.makeText(context, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
                        else ->
                            Toast.makeText(context, "注册失败: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun handleLogin() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "邮箱和密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                    navigateToDrawFragment()
                } else {
                    when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            Toast.makeText(context, "用户不存在", Toast.LENGTH_SHORT).show()
                        "The password is invalid or the user does not have a password." ->
                            Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                        else ->
                            Toast.makeText(context, "登录失败: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
//    private fun navigateToDrawFragment() {
//        parentFragmentManager.commit {
//            replace(R.id.fragmentContainerView, DrawFragment())
//            // 移除返回栈中所有Fragment
//            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
//        }
//    }
    private fun navigateToDrawFragment() {
        val activity = requireActivity() as MainActivity

        activity.tryDownloadFromCloud()

        parentFragmentManager.commit {
            replace(R.id.fragmentContainerView, DrawFragment())
            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}