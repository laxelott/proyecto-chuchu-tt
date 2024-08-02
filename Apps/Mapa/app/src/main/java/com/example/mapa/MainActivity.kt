package com.example.mapa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapa.recoveryPassword.RecoveryPasswordActivity

class MainActivity : AppCompatActivity() {

    //Variable of the login button
    private lateinit var btnLogin: Button

    //Variable of recovery password
    private lateinit var btnRecoveryPassword: TextView

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
            val intent = Intent (this, RecoveryPasswordActivity::class.java)
            startActivity(intent)
        }

        // When the login is success
        btnLogin.setOnClickListener {
            val intent = Intent (this, TransportInformationActivity::class.java)
            startActivity(intent)
        }
    }
}