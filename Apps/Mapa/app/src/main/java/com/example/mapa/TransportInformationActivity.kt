package com.example.mapa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.Serializable

class TransportInformationActivity : AppCompatActivity() {

    //Variables to get the textsViews
    private lateinit var titleTransportType: TextView
    private lateinit var titleTransportRoute: TextView

    //Variable of start button
    private lateinit var btnStart: Button

    //Object with the info of the busStop
    data class BusStop(val name: String, val latitude: Double, val longitude: Double) : Serializable
    //Object with all the line info
    data class TransportInfo(val name: String, val description: String, val color: Long, val busStops: List<BusStop>)
    //Array of objects with all the station lines info
    private lateinit var myTransportInfo: TransportInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transport_information)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Code
        // Getting the TextsViews
        titleTransportType = findViewById(R.id.title_transport_type)
        titleTransportRoute = findViewById(R.id.title_route_transport)

        // Getting the button
        btnStart = findViewById(R.id.btn_start)

        //Getting the data from the DB
        getDataFromDB()

        //Setting the values to the xml
        titleTransportType.text = myTransportInfo.name
        titleTransportRoute.text = myTransportInfo.description
        titleTransportRoute.setBackgroundColor(myTransportInfo.color.toInt())

        // Start the tour
        btnStart.setOnClickListener {
            val intent = Intent (this, MapaActivity::class.java)
            //Pass the busStop to the map
            intent.putExtra("busStops", ArrayList(myTransportInfo.busStops))
            startActivity(intent)
        }
    }

    private fun getDataFromDB (){
        //Array of objects with all the station lines info
        myTransportInfo = TransportInfo("Línea 8", "Circuito Politécnico", 0xFF2B78E4,
            listOf(
                BusStop("Montevideo", 19.493652243084295, -99.14648968274983),
                BusStop("Otalavo", 19.495282977565214, -99.14658271039266),
                BusStop("Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
                BusStop("Politécnico Oriente", 19.50018020355366, -99.14849363636007),
                BusStop("Av Torres", 19.504723274442018, -99.15029433857745),
                BusStop("Av Central", 19.505859948088663, -99.14836046127301),
                BusStop("Juan de Dios Bátiz", 19.50629152440862, -99.14680096929754),
                BusStop("ESCOM", 19.50549012088718, -99.14542972730726),
                BusStop("Ma. Luisa Stampa Ortigoza", 19.504902489030233, -99.14372254004434),
                BusStop("Central de Inteligencia de Cómputo", 19.50465075568148, -99.14219163994267),
                BusStop("Cancha de Entrenamiento Pieles Rojas", 19.50428987354276, -99.1395271781686),
                BusStop("Edif. 11 ESIA", 19.503814773140377, -99.13704964920922),
                BusStop("Manuel de Anda y Barredo", 19.505859948088663, -99.14836046127301),
                BusStop("Edif. 8 ESIQIE", 19.502375940637112, -99.135752104819),
                BusStop("Edif. 6 ESIQIE", 19.501081468018185, -99.13600022103938),
                BusStop("Edif. 4 ESIME", 19.499811455158326, -99.13624965881291),
                BusStop("Edif. 2 ESIME", 19.498540415541733, -99.13649140986477),
                BusStop("Edif. 1 ESIME", 19.497790158201767, -99.13664062421346),
            )
        )
    }
}