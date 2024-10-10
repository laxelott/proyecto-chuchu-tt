package com.example.mapa

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapa.recoveryPassword.RecoveryPasswordActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException
import kotlin.math.log


class MainActivity : AppCompatActivity() {

    // Variables para los elementos UI
    private lateinit var btnLogin: Button
    private lateinit var btnRecoveryPassword: TextView
    private lateinit var username: EditText
    private lateinit var password: EditText

    // SharedPreferences para la sesión
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        checkSession()

        setupUI()
    }

    private fun setupUI() {
        btnLogin = findViewById(R.id.btn_login)
        btnRecoveryPassword = findViewById(R.id.recovery_password_text)
        username = findViewById(R.id.user_name)
        password = findViewById(R.id.user_password)

        btnRecoveryPassword.setOnClickListener {
            val intent = Intent(this, RecoveryPasswordActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            login()
        }
    }

    private fun checkSession() {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            goToTransportInformationActivity()
        }
    }

    private fun login() {
        val user = UserInfo(username.text.toString(), password.text.toString())

        if (user.name.isEmpty() || user.password.isEmpty()) {
            showAlertDialog("¡Campos vacíos!", "Por favor, complete todos los campos antes de continuar.")
        } else {
            makePostRequest(user)
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun makePostRequest(user: UserInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("username", user.name)
                .add("password", user.password)
                .build()

            val request = Request.Builder()
                .url("https://chuchu-backend-w3szgba2ra-vp.a.run.app/api/auth/login")
                .post(formBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                Log.d("driver info", "$response")
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val loginSuccess = Gson().fromJson(responseBody, LoginSuccess::class.java)

                    withContext(Dispatchers.Main) {
                        Log.d("Token", loginSuccess.token)
                        handleLoginResponse(loginSuccess, user)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showAlertDialog("Error", "Error en la solicitud: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showAlertDialog("Error", "Error de red: ${e.message}")
                }
            }
        }
    }

    private fun handleLoginResponse(loginSuccess: LoginSuccess, user: UserInfo) {
        when (loginSuccess.login) {
            0 -> {
                showAlertDialog("¡Credenciales incorrectas!", "Verifica tus credenciales antes de continuar.")
            }
            1 -> {
                saveSession(user.name, loginSuccess.token)
                goToTransportInformationActivity()
            }
            2 -> {
                showAlertDialog("¡Alerta!", "Alguien ya ha iniciado sesión con estas credenciales.")
            }
        }
    }

    private fun saveSession(username: String, token: String) {
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("username", username)
            putString("token", token)
            apply()
        }
    }

    private fun goToTransportInformationActivity() {
        val token = sharedPreferences.getString("token", null)
        Log.d("Token", "$token")
        val intent = Intent(this, TransportInformationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
