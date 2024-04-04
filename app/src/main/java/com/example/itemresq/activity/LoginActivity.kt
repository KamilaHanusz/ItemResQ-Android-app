package com.example.itemresq.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.example.itemresq.databinding.ActivityLoginBinding
import com.example.itemresq.util.UiUtil
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var binding : ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseAuth.getInstance().currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.submitBtn.setOnClickListener{
            logIn()
        }

        binding.goToSignupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            binding.passwordInput.text.clear()
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    fun logIn() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (email.isEmpty()) {
            binding.emailInput.setError("To pole jest obowiązkowe")
            return
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError("Nieprawidłowy adres e-mail")
            return
        }
        if (password.isEmpty()) {
            binding.passwordInput.setError("To pole jest obowiązkowe")
            return
        }

        logInWithFirebase(email, password)
    }

    fun logInWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null && user.isEmailVerified) {
                    UiUtil.showToast(this, "Zalogowano pomyślnie")
                    setInProgress(false)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else if (user != null) {
                    UiUtil.showToast(this, "Adres e-mail nie został zweryfikowany. Kliknij w link weryfikacyjny wysłany na twój adres e-mail.")
                    binding.passwordInput.text.clear()
                    setInProgress(false)
                }
            }
            .addOnFailureListener { exception ->
                UiUtil.showToast(applicationContext, "Nieprawidłowy adres e-mail lub hasło.")
                binding.passwordInput.text.clear()
                setInProgress(false)
            }
    }


    fun setInProgress(inProgress : Boolean) {
        if(inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.submitBtn.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.submitBtn.visibility = View.VISIBLE
        }
    }
}