package com.example.mapa.interfaces

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.mapa.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    @SuppressLint("InflateParams")
    override fun getInfoWindow(marker: Marker): View? {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_infowindow, null)

        val tag = marker.tag as? String
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val button = view.findViewById<Button>(R.id.tv_button)
        tvTitle.text = marker.title
        if (tag == "incident") {
            button.visibility = View.VISIBLE
        }

        return view
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}