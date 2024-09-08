package com.example.mapa

data class TransportInfo(
    val name: String,
    val description: String,
    val color: Long,
    val busStops: List<BusStop>
)