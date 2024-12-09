package com.example.mapa.recoveryPassword

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mapa.MainActivity
import com.example.mapa.R
import com.example.mapa.TokenRequest
import com.example.mapa.interfaces.ApiService
import com.example.pasajero.interfaces.ApiHelper

class RecoveryPasswordActivity : AppCompatActivity() {

    //Variable of the btnBack
    private lateinit var btnBack: TextView

    //Variable of the btnRecovery
    private lateinit var btnRecoveryPassword: Button
    private lateinit var username: EditText


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
        username = findViewById(R.id.user_name)

        // Recovery password
        //TODO send the message to the administrator
        btnBack.setOnClickListener {
            finish()
        }

        // Getting the btn_recovery_password
        btnRecoveryPassword = findViewById(R.id.btn_recovery_password)

        // Back to the login
        btnRecoveryPassword.setOnClickListener {
            val service = ApiHelper().prepareApi()
            val userdata = ApiService.ForgotPassword(username.text.toString())
            ApiHelper().getDataFromDB(
                serviceCall = { service.forgotPassword(userdata) },
                processResponse = { response ->
                    val resp = response.body()
                    if (resp != null) {
                        if (resp.error == 0) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Datos enviados")
                                .setMessage("El administrador recibió tu solicitud.")
                                .setPositiveButton("Ok") { dialog, _ ->
                                    dialog.dismiss()
                                }
                            builder.create().show()
                        }
                        else if (resp.error == 1) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Error")
                                .setMessage("Usuario no encontrado.")
                                .setPositiveButton("Ok") { dialog, _ ->
                                    dialog.dismiss()
                                }
                            builder.create().show()
                        }
                    }
                }
            )
        }
    }
}