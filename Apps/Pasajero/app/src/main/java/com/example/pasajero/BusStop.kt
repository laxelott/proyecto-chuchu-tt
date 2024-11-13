package com.example.pasajero

import java.io.Serializable

data class BusStop (
    var id: Int,
    var name: String,
    var latitude: Double,
    var longitude: Double,
    var waypoints: String? = null
) : Serializable