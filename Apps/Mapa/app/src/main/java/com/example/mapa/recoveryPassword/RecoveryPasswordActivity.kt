package com.example.mapa.recoveryPassword

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapa.MainActivity
import com.example.mapa.R

class RecoveryPasswordActivity : AppCompatActivity() {

    //Variable of the btnBack
    private lateinit var btnBack: TextView

    //Variable of the btnRecovery
    private lateinit var btnRecoveryPassword: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recovery_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Code
        // Getting the textViewBack
        btnBack = findViewById(R.id.textViewBack)

        // Recovery password
        //TODO send the message to the administrator
        btnBack.setOnClickListener {
            val intent = Intent (this, MainActivity::class.java)
            startActivity(intent)
        }

        // Getting the btn_recovery_password
        btnRecoveryPassword = findViewById(R.id.btn_recovery_password)

        // Back to the login
        btnRecoveryPassword.setOnClickListener {
            val intent = Intent (this, PasswordSentActivity::class.java)
            startActivity(intent)
        }
    }
}