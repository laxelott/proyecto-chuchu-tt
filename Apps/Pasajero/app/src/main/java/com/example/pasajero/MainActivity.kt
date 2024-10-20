package com.example.pasajero

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pasajero.interfaces.ApiHelper
import android.util.Base64
import com.example.pasajero.interfaces.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    //Variable for the Linear Layout
    private lateinit var linearLayout: LinearLayout
    private var myTransportInfo: List<TransportInfo> = listOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize API and fetch data
        val service = ApiHelper().prepareApi()
        fetchTransportData(service)
    }

    private fun fetchTransportData(service: ApiService) {
        ApiHelper().getDataFromDB(
            serviceCall = { service.postTransportList() }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val transportInfo = response.body()
                if (transportInfo != null) {
                    Log.d("Transport response", "Datos de transporte recibidos: $transportInfo")
                    myTransportInfo = transportInfo
                    runOnUiThread {
                        linearLayout = findViewById(R.id.layout_transports)
                        createTransportButtons(linearLayout, myTransportInfo)
                    }
                }
            }
        )
    }

    private fun createTransportButtons(
        linearLayout: LinearLayout,
        transportInfo: List<TransportInfo>
    ) {
        for (transport in transportInfo) {
            Log.d("Transport response", "Creando botón para transporte: ${transport.name}")
            val imageButton = createTransportButton(transport)
            linearLayout.addView(imageButton)
        }
    }

    private fun createTransportButton(transport: TransportInfo): ImageButton {
        val imageButton = ImageButton(this)

        /// Verificar si iconB64 no es nulo
        if (transport.icon.isNotEmpty()) {
            try {
                // Decodificar la imagen desde Base64
                val bitmap: Bitmap? = decodeBase64ToBitmap(transport.icon)
                imageButton.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                // Manejar errores de decodificación
                Log.e("Transport response", "Error al decodificar imagen Base64: ${e.message}")
                // Usar una imagen de fallback
                imageButton.setBackgroundResource(R.drawable.trolebus)
            }
        } else {
            Log.d("Transport response", "No se recibió icono Base64. Usando imagen de fallback.")
            // Usar una imagen de fallback si iconB64 está vacío
            imageButton.setBackgroundResource(R.drawable.trolebus)
        }
        val params = LinearLayout.LayoutParams(150,150)
        imageButton.layoutParams = params

        // Set click listener to button
        imageButton.setOnClickListener {
            val intent = Intent(this, SelectedTransportActivity::class.java)
            intent.putExtra("transportID", transport.id)
            intent.putExtra("transportName", transport.name)
            startActivity(intent)
        }

        return imageButton
    }


    private fun decodeBase64ToBitmap(encodedImage: String): Bitmap? {
        // Verifica que el string Base64 no sea nulo ni vacío
        if (encodedImage.isNullOrEmpty()) {
            Log.e("Transport response", "Base64 string is null or empty")
            return null
        }

        // Decodifica la cadena Base64 a un arreglo de bytes
        val decodedString: ByteArray
        try {
            decodedString = Base64.decode(encodedImage, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.e("Transport response", "Failed to decode Base64 string: ${e.message}")
            return null
        }

        // Decodifica el arreglo de bytes a un Bitmap
        return try {
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: Exception) {
            Log.e("Transport response", "Failed to decode byte array to Bitmap: ${e.message}")
            null
        }

    }

}