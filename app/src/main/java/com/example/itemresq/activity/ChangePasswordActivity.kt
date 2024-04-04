package com.example.itemresq.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.itemresq.R
import com.example.itemresq.databinding.ActivityChangePasswordBinding
import com.example.itemresq.util.UiUtil
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    lateinit var binding : ActivityChangePasswordBinding
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isChecked = true
        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isVisible = false

        binding.bottomNavBar.setOnItemSelectedListener {menuItem->
            when(menuItem.itemId) {
                R.id.bottom_menu_main ->{
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.bottom_menu_map ->{
                    startActivity(Intent(this, MapActivity::class.java))
                }
                R.id.bottom_menu_add_report ->{
                    startActivity(Intent(this, AddReportActivity::class.java))
                }
                R.id.bottom_menu_list_reports ->{
                    startActivity(Intent(this, ListAllReportsActivity::class.java))
                }
            }
            false
        }

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.changePasswordBtn.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {

        val currentPassword = binding.currentPasswordInput.text.toString()
        val newPassword = binding.newPasswordInput.text.toString()
        val confirmNewPassword = binding.confirmNewPasswordInput.text.toString()

        if (currentPassword.isEmpty()) {
            binding.currentPasswordInput.setError("To pole jest obowiązkowe")
            return
        }
        if (newPassword.isEmpty()) {
            binding.newPasswordInput.setError("To pole jest obowiązkowe")
            return
        } else if (newPassword.length < 6) {
            binding.newPasswordInput.setError("Hasło musi zawierać minimum 6 znaków")
            return
        }
        if (confirmNewPassword.isEmpty()) {
            binding.confirmNewPasswordInput.setError("To pole jest obowiązkowe")
            return
        }
        if (newPassword != confirmNewPassword) {
            binding.confirmNewPasswordInput.setError("Hasło różni się")
            return
        }

        val user = auth.currentUser
        if (user != null) {
            setInProgress(true)

            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    UiUtil.showToast(this, "Hasło zmienione pomyślnie")
                                    auth.signOut()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                } else {
                                    UiUtil.showToast(this, "Wystąpił błąd. Spróbuj ponownie później")
                                }
                            }
                    } else {
                        binding.currentPasswordInput.setError("Nieprawidłowe hasło")
                    }
                }
            setInProgress(false)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setInProgress(inProgress : Boolean) {
        if(inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.changePasswordBtn.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.changePasswordBtn.visibility = View.VISIBLE
        }
    }
}