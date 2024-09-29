package com.example.mapa

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface GoogleDirectionsApi {
    @GET("directions/json")
    fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") apiKey: String,
        @Query("mode") mode: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("language") language: String? = null // Remove the extra space here
    ): Call<DirectionsResponse>
}

data class DirectionsResponse(
    val geocoded_waypoints: List<GeocodedWaypoint>,
    val routes: List<Route>,
    val status: String
)

data class GeocodedWaypoint(
    val geocoder_status: String,
    val place_id: String,
    val types: List<String>
)

data class Route(
    val bounds: Bounds,
    val copyrights: String,
    val legs: List<Leg>,
    val overview_polyline: OverviewPolyline,
    val summary: String,
    val warnings: List<String>,
    val waypoint_order: List<Int>
)

data class Bounds(
    val northeast: LatLng,
    val southwest: LatLng
)

data class LatLng(
    val lat: Double,
    val lng: Double
)

data class Leg(
    val distance: Distance,
    val duration: Duration,
    val end_address: String,
    val end_location: LatLng,
    val start_address: String,
    val start_location: LatLng,
    val steps: List<Step>,
    val traffic_speed_entry: List<Any>,
    val via_waypoint: List<Any>
)

data class Distance(
    val text: String,
    val value: Int
)

data class Duration(
    val text: String,
    val value: Int
)

data class Step(
    val distance: Distance,
    val duration: Duration,
    val end_location: LatLng,
    val html_instructions: String,
    val polyline: Polyline,
    val start_location: LatLng,
    val travel_mode: String
)

data class Polyline(
    val points: String
)

data class OverviewPolyline(
    val points: String
)
