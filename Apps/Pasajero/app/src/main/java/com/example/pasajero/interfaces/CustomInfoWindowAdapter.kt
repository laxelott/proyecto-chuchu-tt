package com.example.pasajero.interfaces

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.pasajero.BusStop
import com.example.pasajero.InfoMode
import com.example.pasajero.R
import com.example.pasajeropackage.MarkerTag
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    @SuppressLint("InflateParams")
    override fun getInfoWindow(marker: Marker): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_infowindow, null)

        val tag = marker.tag as? MarkerTag
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val button = view.findViewById<Button>(R.id.tv_button)
        
        tvTitle.text = marker.title
        Log.d("Debug", "$tag")
        if (tag is MarkerTag) {
            when (tag.type) {
                "busStop" -> {
                    if (tag.mode == InfoMode.INACTIVE || tag.mode == InfoMode.TARGETED) {
                        button.visibility = View.VISIBLE
                    } else {
                        button.visibility = View.GONE
                    }
                }
                "driverLocation" -> {}
                "incident" -> {}
            }
        }

        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}