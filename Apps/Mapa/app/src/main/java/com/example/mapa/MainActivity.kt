package com.example.mapa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapa.recoveryPassword.RecoveryPasswordActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException


class MainActivity : AppCompatActivity() {

    //Variable for the login button
    private lateinit var btnLogin: Button
    //Variable for recovery password
    private lateinit var btnRecoveryPassword: TextView
    //Variable for the username
    private lateinit var username: EditText
    //Variable for the password
    private lateinit var password: EditText

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
            login()
        }
    }

    private fun login() {
        // Getting the username
        username = findViewById(R.id.user_name)
        // Getting the password
        password = findViewById(R.id.user_password)
        val user = UserInfo(username.text.toString(), password.text.toString())

        if (user.name.isEmpty() || user.password.isEmpty()) {
            // Handle empty fields
            return
        }
        else {
            showProgress(true)
            makePostRequest(user)
        }
    }

    private fun makePostRequest(user: UserInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("username", user.name)
                .add("password", user.password)
                .build()

            val request = Request.Builder()
                .url("https://chuchu-backend-sdhfdsksuq-vp.a.run.app/api/auth/authorize")
                .post(formBody)
                .build()

            try {
                // Execute the network request
                val response = client.newCall(request).execute()

                // Ensure you handle the response in the IO context
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    // Switch back to the Main dispatcher for UI-related tasks
                    withContext(Dispatchers.Main) {
                        println("Response: $responseBody")
                        // Start the next activity after a successful response
                        val intent = Intent(this@MainActivity, TransportInformationActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        println("POST request failed with response code: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    println("POST request failed: ${e.message}")
                }
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