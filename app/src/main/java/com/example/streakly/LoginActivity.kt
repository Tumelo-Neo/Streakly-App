package com.example.streakly

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.streakly.data.HabitManager
import com.example.streakly.databinding.ActivityLoginBinding
import com.example.streakly.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        if (HabitManager.isUserLoggedIn()) {
            navigateToMainActivity()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

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

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                binding.btnLogin.isEnabled = false
                val user = HabitManager.loginUser(email, password)

                if (user != null) {
                    // Login successful
                    HabitManager.setUserSession(user)
                    Toast.makeText(this@LoginActivity, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    // Login failed
                    Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    binding.tilPassword.error = "Invalid credentials"
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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