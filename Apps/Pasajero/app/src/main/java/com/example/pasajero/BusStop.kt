package com.example.pasajero

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class BusStop (
    var id: Int,
    var name: String,
    var latitude: Double,
    var longitude: Double
) : Serializable
