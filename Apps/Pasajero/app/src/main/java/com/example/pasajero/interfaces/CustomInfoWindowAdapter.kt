package com.example.pasajero.interfaces

import android.content.Context
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.pasajero.BusStop
import com.example.pasajero.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context, private val currentLocation: LatLng, private val busStops: List<BusStop>) : GoogleMap.InfoWindowAdapter {

    // Método para inflar el layout del InfoWindow
    override fun getInfoWindow(marker: Marker): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_infowindow, null)

        // Encuentra los elementos de la vista y configura el contenido
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        tvTitle.text = marker.title

        val btnFindNearest = view.findViewById<Button>(R.id.tv_button)
        btnFindNearest?.setOnClickListener {
            Log.d("Transport info", "Click al button de custom window")
            findNearestBusStop(marker)
        }

        // Agregar más elementos según sea necesario
        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        // Si deseas personalizar el contenido cuando se hace clic en el marcador,
        // puedes implementar esta función. Si no es necesario, puedes devolver null.
        return null
    }

    private fun findNearestBusStop(marker: Marker) {
        var nearestBusStop: BusStop? = null
        var minDistance = Float.MAX_VALUE

        // Loop through all BusStops to find the nearest
        for (busStop in busStops) {
            val busStopLocation = Location("BusStop").apply {
                latitude = busStop.latitude
                longitude = busStop.longitude
            }

            val distance = busStopLocation.distanceTo(Location("CurrentLocation").apply {
                latitude = currentLocation.latitude
                longitude = currentLocation.longitude
            })

            if (distance < minDistance) {
                minDistance = distance
                nearestBusStop = busStop
            }
        }

        // Now you can show the nearestBusStop information
        nearestBusStop?.let {
            // You can show a Toast or update the UI with the nearest BusStop details
            Toast.makeText(context, "Nearest Bus Stop: ${it.name}, Distance: ${minDistance}m", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(context, "No Bus Stops available", Toast.LENGTH_SHORT).show()
    }
}