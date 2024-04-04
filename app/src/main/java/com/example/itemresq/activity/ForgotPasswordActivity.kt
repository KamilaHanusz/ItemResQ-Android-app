package com.example.itemresq.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import com.example.itemresq.databinding.ActivityForgotPasswordBinding
import com.example.itemresq.util.UiUtil
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    lateinit var binding : ActivityForgotPasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.resetPasswordBtn.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        val email = binding.emailInput.text.toString()
        if (email.isEmpty()) {
            binding.emailInput.setError("To pole jest obowiązkowe")
            return
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError("Nieprawidłowy adres e-mail")
            return
        }
        auth.sendPasswordResetEmail(email).addOnCompleteListener {
            if (it.isSuccessful) {
                UiUtil.showToast(this, "Na twój adres e-mail został wysłany link do zresetowania hasła")
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

}