package com.example.mapa

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pasajero.interfaces.ApiHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.log


class TransportInformationActivity : AppCompatActivity() {

    // Variables para las vistas
    private lateinit var titleTransportType: TextView
    private lateinit var titleTransportRoute: TextView
    private lateinit var spinnerBusOptions: Spinner
    private lateinit var btnStart: Button
    private lateinit var btnLogout: TextView

    // Token del conductor (recuperado de la sesión)
    private lateinit var conductorToken: String
    private var allDriverInfo: List<DriverInfo> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transport_information)
        setupUI()
        checkUserSession()
        loadDriverInfo()
        btnStart.isEnabled = false
    }

    private fun setupUI() {
        titleTransportType = findViewById(R.id.title_transport_type)
        titleTransportRoute = findViewById(R.id.title_route_transport)
        spinnerBusOptions = findViewById(R.id.spinner_options)
        btnStart = findViewById(R.id.btn_start)
        btnLogout = findViewById(R.id.btn_logout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLogout.setOnClickListener {
            showAlertDialog()
        }
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("token", conductorToken)
                .build()

            val request = Request.Builder()
                .url("https://chuchu-backend-w3szgba2ra-vp.a.run.app/api/auth/logout")
                .post(formBody)
                .build()


            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val listType = object : TypeToken<List<LogoutSuccess>>() {}.type
                    val loginSuccessList: List<LogoutSuccess> = Gson().fromJson(responseBody, listType)


                    withContext(Dispatchers.Main) {
                        when (loginSuccessList.first().logout) {
                            "0" -> {
                                val intent = Intent(this@TransportInformationActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            "1" -> {
                                val intent = Intent(this@TransportInformationActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("Logout", "Fallo en logout")
                    }
                }
            } catch (e: IOException) {
                Log.e("Logout", "Error ${e.message}")
            }
        }
    }


    private fun showAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Deseas cerrar sesión?")
            .setPositiveButton("Si") { dialog, _ ->
                logout()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun checkUserSession() {
        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        conductorToken = sharedPreferences.getString("token", "") ?: ""

        if (conductorToken.isEmpty()) {
            showErrorDialog("Sesión no iniciada. Por favor, inicia sesión.")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadDriverInfo() {
        val service = ApiHelper().prepareApi()
        val tokenRequest = TokenRequest(conductorToken)
        ApiHelper().getDataFromDB(
            serviceCall = { service.getVehicles(tokenRequest) },
            processResponse = { response ->
                val infoVehicles = response.body()
                if (infoVehicles != null) {
                    allDriverInfo = infoVehicles
                    Log.e("driver info", "$infoVehicles")
                    updateUIWithDriverInfo(allDriverInfo)
                } else {
                    showErrorDialog("Error al cargar la información del conductor.")
                }
            }
        )
    }

    private fun updateUIWithDriverInfo(allInfo: List<DriverInfo>) {
        val options: MutableList<String> = mutableListOf("Selecciona una opción")
        for (driverInfo in allInfo) {
            options += driverInfo.vehicleIdentifier
        }

        titleTransportType.text = allInfo[0].transportName
        val color = Color.parseColor("#"+allInfo[0].routeColor)
        titleTransportRoute.setBackgroundColor(color)
        titleTransportRoute.text = allInfo[0].routeName


        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBusOptions.adapter = adapter
        btnStart.isEnabled = false
        spinnerBusOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedOption = options[position]
                btnStart.isEnabled = selectedOption != "Selecciona una opción"
                if (btnStart.isEnabled) {
                    val idroute = allInfo[0].idRoute
                    setupStartButton(selectedOption, idroute)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                btnStart.isEnabled = false
            }
        }
    }

    private fun setupStartButton(selectedOption: String, idroute: Int) {
        btnStart.setOnClickListener {


            useVehicle(conductorToken, selectedOption)

            val intent = Intent(this, MapaActivity::class.java)
            intent.putExtra("token", conductorToken)
            intent.putExtra("idRoute", idroute)
            intent.putExtra("vehicleIdentifier", selectedOption)
            startActivity(intent)
        }
    }

    private fun useVehicle(token:String, vehicleIdentifier: String) {
        val service = ApiHelper().prepareApi()
        val tokenRequest = TokenRequest(token)
        ApiHelper().getDataFromDB(
            serviceCall = { service.postUseVehicle(vehicleIdentifier, tokenRequest) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val responseBody = response.body()
                Log.d("Response", "$responseBody")
                if (responseBody != null) {
                    when (responseBody.error) {
                        0 -> {
                        }
                        2 -> {
                            newAlertDialog("¡Alerta!", "Este vehículo ya está en uso")
                        }
                    }
                }
            }
        )
    }

    private fun newAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
