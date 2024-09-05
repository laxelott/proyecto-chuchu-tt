package com.example.pasajero

import android.app.ActionBar.LayoutParams
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pasajero.databinding.ActivitySelectedTransportBinding
import java.io.Serializable

class SelectedTransportActivity : AppCompatActivity() {

    //Variable for the textViewBack
    private lateinit var btnBack: TextView
    //Variable for the textViewTitle
    private lateinit var titleTransport: TextView

    private lateinit var adapter: LineInfoAdapter
    private lateinit var binding: ActivitySelectedTransportBinding

    //Array of objects with all the station lines info
    private val stationLines = arrayListOf<LineInfo>()

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

        //Getting the textViewTitle
        titleTransport = findViewById(R.id.textViewTitle)
        //Setting the text
        titleTransport.text = "Trolebús"

        llenarLista()
        setupRecyclerView()

    }

    fun setupRecyclerView() {
        binding.rvLineas.layoutManager = LinearLayoutManager(this)
        adapter = LineInfoAdapter(stationLines)
        binding.rvLineas.adapter = adapter
    }

    fun llenarLista() {
        stationLines.add(
            LineInfo(1, "Línea 1", "Central del norte - Central del Sur",
                listOf(
                    BusStop(1, "Montevideo", 19.493652243084295, -99.14648968274983),
                    BusStop(2, "Otalavo", 19.495282977565214, -99.14658271039266),
                    BusStop(3, "Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
                    BusStop(4, "Politécnico Oriente", 19.50018020355366, -99.14849363636007),
                    BusStop(5, "Av Torres", 19.504723274442018, -99.15029433857745),
                    BusStop(6, "Av Central", 19.505859948088663, -99.14836046127301)
                )
            ))
        stationLines.add(
            LineInfo(2, "Línea 2", "Chapultepec - Pantitlán",
                listOf(
                    BusStop(1, "Montevideo", 19.493652243084295, -99.14648968274983),
                    BusStop(2, "Otalavo", 19.495282977565214, -99.14658271039266),
                    BusStop(3, "Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
                    BusStop(4, "Politécnico Oriente", 19.50018020355366, -99.14849363636007),
                    BusStop(5, "Av Torres", 19.504723274442018, -99.15029433857745),
                    BusStop(6, "Av Central", 19.505859948088663, -99.14836046127301)
                )
            ))
        stationLines.add(
            LineInfo(3,"Línea 8", "Circuito Politécnico",
                listOf(
                    BusStop(1,"Montevideo", 19.493652243084295, -99.14648968274983),
                    BusStop(2,"Otalavo", 19.495282977565214, -99.14658271039266),
                    BusStop(3,"Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
                    BusStop(4,"Politécnico Oriente", 19.50018020355366, -99.14849363636007),
                    BusStop(5,"Av Torres", 19.504723274442018, -99.15029433857745),
                    BusStop(6,"Av Central", 19.505859948088663, -99.14836046127301),
                    BusStop(7,"Juan de Dios Bátiz", 19.50629152440862, -99.14680096929754),
                    BusStop(8,"ESCOM", 19.50549012088718, -99.14542972730726),
                    BusStop(9,"Ma. Luisa Stampa Ortigoza", 19.504902489030233, -99.14372254004434),
                    BusStop(10,"Central de Inteligencia de Cómputo", 19.50465075568148, -99.14219163994267),
                    BusStop(11,"Cancha de Entrenamiento Pieles Rojas", 19.50428987354276, -99.1395271781686),
                    BusStop(12,"Edif. 11 ESIA", 19.503814773140377, -99.13704964920922),
                    BusStop(13,"Manuel de Anda y Barredo", 19.505859948088663, -99.14836046127301),
                    BusStop(14,"Edif. 8 ESIQIE", 19.502375940637112, -99.135752104819),
                    BusStop(15,"Edif. 6 ESIQIE", 19.501081468018185, -99.13600022103938),
                    BusStop(16,"Edif. 4 ESIME", 19.499811455158326, -99.13624965881291),
                    BusStop(17,"Edif. 2 ESIME", 19.498540415541733, -99.13649140986477),
                    BusStop(18,"Edif. 1 ESIME", 19.497790158201767, -99.13664062421346),
                )
            ))
    }
}