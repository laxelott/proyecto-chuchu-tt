package com.example.mapa

data class Incident (
    val id: Int,
    val name: String,
    val type: String,
    val description: String,
    val lat: Double,
    val lon: Double
)