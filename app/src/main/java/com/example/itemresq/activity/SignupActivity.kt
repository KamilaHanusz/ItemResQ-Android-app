package com.example.itemresq.activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.itemresq.databinding.ActivitySignupBinding
import com.example.itemresq.model.UserModel
import com.example.itemresq.util.UiUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    lateinit var binding : ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submitBtn.setOnClickListener{
            signUp()
        }

        binding.goToLoginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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

    fun signUp() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

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
        if(password.length < 6) {
            binding.passwordInput.setError("Hasło musi zawierać minimum 6 znaków")
            return
        }
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.setError("To pole jest obowiązkowe")
            return
        }
        if(password != confirmPassword) {
            binding.confirmPasswordInput.setError("Hasło różni się")
            return
        }

        createAccountInFirebase(email, password)
    }

    fun createAccountInFirebase(email: String, password: String) {
        setInProgress(true)

        val firebaseAuth = FirebaseAuth.getInstance()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let { user ->
                    val userModel = UserModel(user.uid, email)
                    Firebase.firestore.collection("users")
                        .document(user.uid)
                        .set(userModel)
                        .addOnSuccessListener {
                            user.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        setInProgress(false)
                                        UiUtil.showToast(applicationContext, "Konto utworzone pomyślnie. Kliknij w link weryfikacyjny wysłany na twój adres e-mail.")
                                        firebaseAuth.signOut()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    } else {
                                        UiUtil.showToast(applicationContext, "Wystąpił błąd podczas wysyłania linku weryfikacyjnego.")
                                        clearFormFields()
                                    }
                                }
                        }
                }
            }
            .addOnFailureListener { exception ->
                if (exception is FirebaseAuthUserCollisionException) {
                    UiUtil.showToast(applicationContext, "Istnieje już konto przypisane do tego adresu e-mail.")
                    clearFormFields()
                } else {
                    UiUtil.showToast(this, "Wystąpił błąd.")
                    clearFormFields()
                }
                setInProgress(false)
            }
    }

    private fun clearFormFields() {
        binding.emailInput.text.clear()
        binding.passwordInput.text.clear()
        binding.confirmPasswordInput.text.clear()
    }
}