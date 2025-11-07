package com.example.streakly

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.streakly.data.HabitManager
import com.example.streakly.databinding.ActivityRegisterBinding
import com.example.streakly.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegistration()
            }
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        if (binding.etName.text.toString().trim().isEmpty()) {
            binding.tilName.error = "Name is required"
            isValid = false
        } else {
            binding.tilName.error = null
        }

        // Validate email
        if (binding.etEmail.text.toString().trim().isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!isValidEmail(binding.etEmail.text.toString().trim())) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validate password
        if (binding.etPassword.text.toString().trim().isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (binding.etPassword.text.toString().length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        // Validate confirm password
        if (binding.etConfirmPassword.text.toString().trim().isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (binding.etPassword.text.toString() != binding.etConfirmPassword.text.toString()) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                binding.btnRegister.isEnabled = false
                val success = HabitManager.registerUser(name, email, password)

                if (success) {
                    Toast.makeText(this@RegisterActivity, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                    binding.tilEmail.error = "Email already exists"
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun applySavedLanguage() {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("app_language", "en") ?: "en"

        val locale = Locale(savedLanguage)
        Locale.setDefault(locale)

        val resources = resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            applicationContext.createConfigurationContext(configuration)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}