package com.example.pasajero

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Xml
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.pasajero.interfaces.ApiHelper
import com.example.pasajero.interfaces.ApiService
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

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
            serviceCall = { service.postTransportList() }, // Pass the function that makes the request
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

    private fun loadVectorDrawableFromBase64(context: Context, base64String: String): Drawable? {
        return try {
            // Decodificar la cadena Base64
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

            // Crear un archivo temporal en el almacenamiento interno
            val file = File(context.filesDir, "temp_vector.xml")
            FileOutputStream(file).use { fos ->
                fos.write(decodedBytes)
            }

            // Crear un XmlPullParser para leer el archivo
            val inputStream = file.inputStream()
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(inputStream, null)

            // Avanzar al primer tag que es <vector>
            parser.next()

            // Cargar el VectorDrawable desde el InputStream
            val drawable = VectorDrawableCompat.createFromXml(context.resources, parser)

            // Eliminar el archivo después de usarlo
            file.delete()

            drawable
        } catch (e: Exception) {
            Log.e("Transport response", "Error al cargar VectorDrawable desde Base64: ${e.message}", e)
            null
        }
    }

    // Método adaptado para crear el ImageButton con el ícono decodificado
    private fun createTransportButton(transport: TransportInfo): ImageButton {
        val imageButton = ImageButton(this)

        // Verificar si el icono no es nulo o vacío
        if (transport.icon.isNotEmpty()) {
            try {
                val drawable = loadVectorDrawableFromBase64(this, transport.icon)
                drawable?.let {
                    imageButton.setImageDrawable(it)
                } ?: run {
                    // Usar una imagen de fallback si no se pudo cargar el drawable
                    imageButton.setBackgroundResource(R.drawable.trolebus)
                }
            } catch (e: IllegalArgumentException) {
                // Manejar errores de decodificación
                Log.e("Transport response", "Error al decodificar imagen Base64: ${e.message}")
                // Usar una imagen de fallback
                imageButton.setBackgroundResource(R.drawable.trolebus)
            }
        } else {
            Log.d(" Transport response", "No se recibió icono Base64. Usando imagen de fallback.")
            // Usar una imagen de fallback si el icono está vacío
            imageButton.setBackgroundResource(R.drawable.trolebus)
        }

        // Configurar el tamaño del botón
        val params = LinearLayout.LayoutParams(150, 150)
        imageButton.layoutParams = params

        // Establecer listener para el botón
        imageButton.setOnClickListener {
            val intent = Intent(this, SelectedTransportActivity::class.java)
            intent.putExtra("transportID", transport.id)
            intent.putExtra("transportName", transport.name)
            startActivity(intent)
        }

        return imageButton
    }
}