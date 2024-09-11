package com.example.pasajero

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pasajero.databinding.ActivitySelectedTransportBinding
import com.example.pasajero.interfaces.ApiHelper

class SelectedTransportActivity : AppCompatActivity() {

    //Variable for the textViewBack
    private lateinit var btnBack: TextView
    //Variable for the textViewTitle
    private lateinit var titleTransport: TextView

    private lateinit var adapter: LineInfoAdapter
    private lateinit var binding: ActivitySelectedTransportBinding
    private var transportID: Int = 0
    private var transportName: String = ""
    private var myRoutesInfo: List<LineInfo> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectedTransportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Code
        //Getting the textViewBack
        btnBack = findViewById(R.id.textViewBack)
        //Back to home
        btnBack.setOnClickListener {
            val intent = Intent (this, MainActivity::class.java)
            startActivity(intent)
        }

        // Getting the transportID
        transportID = intent.getIntExtra("transportID", 0)
        transportName = intent.getStringExtra("transportName")?: "Unknown"

        // Get all the routes
        // api/data/route/list/idTransport
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.postRoutesInfo(transportID) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val routesInfo = response.body()
                if (routesInfo != null) {
                    Log.d("Transport response", "Datos de transporte: $routesInfo")
                    runOnUiThread {
                        myRoutesInfo = routesInfo
                        //Getting the textViewTitle
                        titleTransport = findViewById(R.id.textViewTitle)
                        //Setting the text
                        titleTransport.text = transportName
                        setupRecyclerView(myRoutesInfo)
                    }
                }
            }
        )
    }

    private fun setupRecyclerView(myRoutesInfo: List<LineInfo>) {
        binding.rvLineas.layoutManager = LinearLayoutManager(this)
        adapter = LineInfoAdapter(myRoutesInfo)
        binding.rvLineas.adapter = adapter
    }


//        myTransportInfo = TransportInfo("Trolebús",
//            listOf(
//                LineInfo(1,"Línea 8", "Circuito Politécnico", 0xFF2B78E4,
//                    listOf(
//                        BusStop(1,"Montevideo", 19.493652243084295, -99.14648968274983),
//                        BusStop(2, "Otalavo", 19.495282977565214, -99.14658271039266),
//                        BusStop(3,"Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
//                        BusStop(4, "Politécnico Oriente", 19.50018020355366, -99.14849363636007),
//                        BusStop(5, "Av Torres", 19.504723274442018, -99.15029433857745),
//                        BusStop(6, "Av Central", 19.505893273106125, -99.14835222461627),
//                        BusStop(7, "Juan de Dios Bátiz", 19.50629152440862, -99.14680096929754),
//                        BusStop(8, "ESCOM", 19.50549012088718, -99.14542972730726),
//                        BusStop(9, "Ma. Luisa Stampa Ortigoza", 19.504963890866264, -99.1436944408264),
//                        BusStop(10, "Central de Inteligencia de Cómputo", 19.50465075568148, -99.14219163994267),
//                        BusStop(11, "Cancha de Entrenamiento Pieles Rojas", 19.504189745349223, -99.13945605909623),
//                        BusStop(12,"Edif. 11 ESIA", 19.503776833138303, -99.13706355059324),
//                        BusStop(13, "Manuel de Anda y Barredo", 19.503566177884437, -99.1358099112321),
//                        BusStop(14,"Edif. 8 ESIQIE", 19.502375940637112, -99.135752104819),
//                        BusStop(15,"Edif. 6 ESIQIE", 19.501081468018185, -99.13600022103938),
//                        BusStop(16,"Edif. 4 ESIME", 19.499811455158326, -99.13624965881291),
//                        BusStop(17,"Edif. 2 ESIME", 19.498540415541733, -99.13649140986477),
//                        BusStop(18,"Edif. 1 ESIME", 19.497790158201767, -99.13664062421346),
//                        BusStop(19,"Edif. 1 ESIME", 19.497761542197438, -99.13646064358117),
//                        BusStop(20,"Edif. 2 ESIME", 19.498507036578488,-99.13630786418547),
//                        BusStop(21,"Edif. 4 ESIME", 19.499789712695986, -99.13605993626295),
//                        BusStop(22,"Edif. 6 ESIQIE", 19.501048977199606, -99.13582085865777),
//                        BusStop(23,"Edif. 8 ESIQIE", 19.502342079193873, -99.13557448148346),
//                        BusStop(24,"Edif. 10 ESIA", 19.5038802007935, -99.13577003024044),
//                        BusStop(25,"Edif. 11 ESIA", 19.5040395540449, -99.13691071038902),
//                        BusStop(26,"Biblioteca ESIA", 19.504288859244497, -99.13841049432415),
//                        BusStop(27,"Secretaría de Extensión y Difusión", 19.504520067025705, -99.13973006902111),
//                        BusStop(28,"Central de Inteligencia de Cómputo", 19.504939377645854, -99.1421400067882),
//                        BusStop(29,"Ma. Luisa Stampa Ortigoza", 19.50511408286138, -99.14318104068121),
//                        BusStop(30,"ESCOM", 19.5055394802009, -99.14510976799576),
//                        BusStop(31,"Av. Juan de Dios Bátiz", 19.506459568806815, -99.14648967979731),
//                        BusStop(32,"J. Othón de Medizabal", 19.50450859749287, -99.15116018472465),
//                        BusStop(33,"Politécnico", 19.500535947911718, -99.14959530848424),
//                    )
//                )
//            )
//        )



}
