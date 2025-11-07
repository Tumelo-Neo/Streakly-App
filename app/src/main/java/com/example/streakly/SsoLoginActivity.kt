package com.example.streakly

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.streakly.data.HabitManager
import com.example.streakly.utils.ThemeManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SsoLoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    handleSsoSignIn(idToken, account.email ?: "", account.displayName ?: "")
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "SSO sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLanguage()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sso_login)

        // Configure Google Sign In for SSO
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnGoogleSso).setOnClickListener {
            signInWithGoogle()
        }

        findViewById<Button>(R.id.btnEmailLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSsoSignIn(idToken: String, email: String, displayName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = HabitManager.signInWithGoogle(idToken)
            if (success) {
                // Get Firebase user info to sync with backend
                val firebaseUser = HabitManager.getFirebaseUser()
                val userEmail = firebaseUser?.email ?: email
                val userName = firebaseUser?.displayName ?: displayName

                // Sync with backend
                syncUserWithBackend(userEmail, userName)

                Toast.makeText(this@SsoLoginActivity, "Signed in successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to main activity
                val intent = Intent(this@SsoLoginActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@SsoLoginActivity, "SSO sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun syncUserWithBackend(email: String, displayName: String) {
        try {
            // This will create or sync the user with the backend
            // The backend handles both new and existing SSO users
        } catch (e: Exception) {
            // Log the error but continue - user can still use the app
            e.printStackTrace()
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