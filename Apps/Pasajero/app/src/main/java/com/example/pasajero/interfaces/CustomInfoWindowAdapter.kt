package com.example.pasajero.interfaces

import android.annotation.SuppressLint
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

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    @SuppressLint("InflateParams")
    override fun getInfoWindow(marker: Marker): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_infowindow, null)

        val tag = marker.tag as? String
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val button = view.findViewById<Button>(R.id.tv_button)
        when (tag) {
            "busStop" -> {
                tvTitle.text = marker.title
                button.visibility = View.VISIBLE
            }
            "driverLocation" -> {
                tvTitle.text = marker.title
                
            }
        }

        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}