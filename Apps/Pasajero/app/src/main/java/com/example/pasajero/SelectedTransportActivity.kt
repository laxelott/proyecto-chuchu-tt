package com.example.pasajero

import android.app.ActionBar.LayoutParams
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import java.io.Serializable

class SelectedTransportActivity : AppCompatActivity() {

    //Variable for the textViewBack
    private lateinit var btnBack: TextView
    //Variable for the textViewTitle
    private lateinit var titleTransport: TextView
    //Variable for the linearLayoutAllLines
    private lateinit var allLinesContainer: LinearLayout

    //Object with the info of the busStop
    data class BusStop(val name: String, val latitude: Double, val longitude: Double) : Serializable
    //Object with all the line info
    data class LineInfo(val name: String, val description: String, val busStops: List<BusStop>)

    //Array of objects with all the station lines info
    private val stationLines = arrayOf(
        LineInfo("Línea 1", "Central del norte - Central del Sur",
            listOf(
                BusStop("Montevideo", 19.493652243084295, -99.14648968274983),
                BusStop("Otalavo", 19.495282977565214, -99.14658271039266),
                BusStop("Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
                BusStop("Politécnico Oriente", 19.50018020355366, -99.14849363636007),
                BusStop("Av Torres", 19.504723274442018, -99.15029433857745),
                BusStop("Av Central", 19.505859948088663, -99.14836046127301)
            )
        ),
        LineInfo("Línea 2", "Chapultepec - Pantitlán",
            listOf(
                BusStop("Montevideo", 19.493652243084295, -99.14648968274983),
                BusStop("Otalavo", 19.495282977565214, -99.14658271039266),
                BusStop("Wilfrido Massieu", 19.499001984126647, -99.14804302526532),
                BusStop("Politécnico Oriente", 19.50018020355366, -99.14849363636007),
                BusStop("Av Torres", 19.504723274442018, -99.15029433857745),
                BusStop("Av Central", 19.505859948088663, -99.14836046127301)
            )
        ),
        LineInfo("Línea 8", "Circuito Politécnico",
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
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_selected_transport)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        //Getting the linearLayoutLines
        allLinesContainer = findViewById(R.id.linearLayoutAllLines)




        //TODO: repeat this to create the number of lines wanted
        for ((i,stationLine) in stationLines.withIndex()) {
            createCard(stationLine)
            if (i < stationLines.size){
                //Adding space between the infoContainer elements
                val space = Space(this)
                space.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    30
                )
                allLinesContainer.addView(space)
            }
        }

    }

    /**
     * Create a cardView with the info about the line of the transport
     *
     * @param lineName name of the line
     * @param startEnd from where to where
     * @return nothing
     */
    private fun createCard(stationLine: LineInfo){
        //Creating the CardView for design
        val cardViewContainer = CardView(this)
        cardViewContainer.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        cardViewContainer.radius = 10f
        cardViewContainer.cardElevation = 6f
        cardViewContainer.setCardBackgroundColor(Color.parseColor("#EAEAEA"))
        cardViewContainer.isClickable = true

        //Creating the lineInfoContainer with their properties
        val infoContainer = LinearLayout(this)
        infoContainer.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        infoContainer.orientation = LinearLayout.HORIZONTAL
        infoContainer.gravity = Gravity.CENTER_VERTICAL
        //Adding the info container to the LinesContainer
        infoContainer.setPadding(8)
        //Adding infoContainer to cardViewContainer
        cardViewContainer.addView(infoContainer)

        //Creating the location image
        val locationImage = ImageView(this)
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(8,0,8,0)
        locationImage.layoutParams = params
        locationImage.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.location,null))
        //Adding the image to the infoContainer
        infoContainer.addView(locationImage)

        //Creating the text container
        val textContainer = LinearLayout(this)
        textContainer.orientation = LinearLayout.VERTICAL
        textContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        //Adding the textContainer to the infoContainer
        infoContainer.addView(textContainer)

        //Creating the title for the infoContainer
        val textTitle = TextView(this)
        textTitle.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textTitle.typeface = ResourcesCompat.getFont(this,R.font.poppins_semibold)
        textTitle.textSize = 16F
        textTitle.text = stationLine.name
        textTitle.setTextColor(Color.parseColor("#273431"))
        //Adding to the textContainer
        textContainer.addView(textTitle)

        //Creating the subtitle for the infoContainer
        val textSubtitle = TextView(this)
        textSubtitle.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        textSubtitle.typeface = ResourcesCompat.getFont(this,R.font.poppins)
        textSubtitle.textSize = 12F
        textSubtitle.text = stationLine.description
        textSubtitle.setTextColor(Color.parseColor("#7A908A"))
        //Adding to the textContainer
        textContainer.addView(textSubtitle)

        //onClick event to the cardView
        cardViewContainer.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("busStops", ArrayList(stationLine.busStops))
            startActivity(intent)
        }

        //Adding the cardViewContainer to the allLinesContainer
        allLinesContainer.addView(cardViewContainer)


    }
}