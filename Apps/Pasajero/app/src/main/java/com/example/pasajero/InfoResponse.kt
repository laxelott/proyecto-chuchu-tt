package com.example.pasajero

data class
InfoResponse (
    val error: Int,
    val identifier: String,
    val nextName: String,
    val nextDistance: Float,
    val nextTime: Float,
    val totalDistance: Float,
    val totalTime: Float,
    val message: String,
    val arrived: Int
)