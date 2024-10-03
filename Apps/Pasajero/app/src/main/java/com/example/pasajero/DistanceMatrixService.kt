package com.example.pasajero

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit service for Google Distance Matrix API
interface DistanceMatrixService {
    @GET("distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,         // Current location
        @Query("destinations") destinations: String, // List of destinations (stations)
        @Query("key") apiKey: String,
        @Query("mode") mode: String    // Set mode to walking
    ): Response<DistanceMatrixResponse>
}

data class DistanceMatrixResponse(
    val rows: List<DistanceMatrixRow>
)

data class DistanceMatrixRow(
    val elements: List<DistanceMatrixElement>
)

data class DistanceMatrixElement(
    val distance: Distance,
    val duration: Duration
)

data class Distance(
    val value: Int,
    val text: String
)

data class Duration(
    val value: Int,
    val text: String
)