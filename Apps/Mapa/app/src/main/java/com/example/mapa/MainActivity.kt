package com.example.mapa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapa.interfaces.ApiService
import com.example.mapa.interfaces.LoginResponse
import com.example.mapa.interfaces.User
import com.example.mapa.recoveryPassword.RecoveryPasswordActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    //Variable for the login button
    private lateinit var btnLogin: Button
    //Variable for recovery password
    private lateinit var btnRecoveryPassword: TextView
    //Variable for the username
    private lateinit var username: EditText
    //Variable for the password
    private lateinit var password: EditText
    // Retrofit instance (initialized once)
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://chuchu-backend-sdhfdsksuq-vp.a.run.app/") // Replace with your URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    //OnCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Code
        // Getting the button
        btnLogin = findViewById(R.id.btn_login)

        // Getting the recovery password button
        btnRecoveryPassword = findViewById(R.id.recovery_password_text)

        // To recover the password
        btnRecoveryPassword.setOnClickListener {
            val intent = Intent(this, RecoveryPasswordActivity::class.java)
            startActivity(intent)
        }

        // When the login is success
        btnLogin.setOnClickListener {
            val intent = Intent(this, TransportInformationActivity::class.java)
            startActivity(intent)
            //login()
        }
    }

    private fun login() {
        // Getting the username
        username = findViewById(R.id.user_name)
        // Getting the password
        password = findViewById(R.id.user_password)
        val user = User(username.text.toString(), password.text.toString())

        if (user.name.isEmpty() || user.password.isEmpty()) {
            // Handle empty fields
            return
        }

        showProgress(true)

        val service = retrofit.create(ApiService::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loginResponse: Response<LoginResponse> = service.login(user) // Assuming service.login is suspend
                if (loginResponse.isSuccessful) {
                    // Handle successful login (e.g., navigate to next activity)
                    showSuccessMessage()
                    navigateToNextActivity()
                } else {
                    // Handle error in login response (e.g., show error message)
                    showError("Falló el login")
                }
            } catch (e: Exception) {
                // Handle network or other exceptions
                showError(getString(R.string.error_network))
            } finally {
                showProgress(false)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        // Implement logic to show or hide a progress indicator
        // For example, you could use a ProgressBar or a loading spinner
    }

    private fun showSuccessMessage() {
        // Implement logic to show a success message
        // For example, you could use a Toast or a SnackBar
    }

    private fun showError(message: String) {
        // Implement logic to show an error message
        // For example, you could use a Toast or a SnackBar
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, TransportInformationActivity::class.java)
        startActivity(intent)
    }
}