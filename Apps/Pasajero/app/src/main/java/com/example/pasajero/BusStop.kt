package com.example.pasajero

import com.google.android.gms.maps.model.LatLng
import java.io.Serializable

data class BusStop (
    var id: Int,
    var name: String,
    var latitude: Double,
    var longitude: Double,
    var waypoints: String?  // Holds the parsed LatLng list
) : Serializable