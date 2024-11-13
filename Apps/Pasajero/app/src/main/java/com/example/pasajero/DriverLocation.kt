package com.example.pasajero

data class DriverLocation (
    val identifier: String,
    val lon: Double,
    val lat: Double,
    val direction: Float,
) {
    override fun equals(other: Any?): Boolean {
        return (other is DriverLocation) && identifier == other.identifier
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}