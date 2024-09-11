package com.example.pasajero

class LineInfo (
    var id: Int,
    var name: String,
    var description: String,
    val color: Long,
    val busStops: List<BusStop>
)