package com.example.mapa

import java.io.Serializable

data class BusStop(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var waypoints: String? = null // Holds the parsed LatLng list
) :Serializable
