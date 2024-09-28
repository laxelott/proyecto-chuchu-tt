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

        // Agregar más elementos según sea necesario
        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        // Si deseas personalizar el contenido cuando se hace clic en el marcador,
        // puedes implementar esta función. Si no es necesario, puedes devolver null.
        return null
    }
}