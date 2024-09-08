package com.example.mapa

import java.io.Serializable

data class BusStop(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double
) :Serializable
