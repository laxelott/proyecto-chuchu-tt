package com.example.mapa.interfaces


import com.example.mapa.BusStop
import com.example.mapa.DriverInfo
import com.example.mapa.GenericResponse
import com.example.mapa.Incident
import com.example.mapa.InfoResponse
import com.example.mapa.TokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    /*
    * api/data/transport/list
    * api/data/route/list/idTransport
    * api/data/stop/list/idRoute
    * */
    @POST("driver/getInfo")
    suspend fun getVehicles(@Body token: TokenRequest): Response<List<DriverInfo>>

    @POST("driver/useVehicle/{vehicleIdentifier}")
    suspend fun postUseVehicle(
        @Path("vehicleIdentifier") idVehicle: String,
        @Body token: TokenRequest
    ): Response<GenericResponse>

    @POST("driver/leaveVehicle/")
    suspend fun leaveVehicle(@Body token: TokenRequest): Response<GenericResponse>

    @POST("data/stop/list/{idRoute}")
    suspend fun getBusStopsInfo(@Path("idRoute") idRoute: Int): Response<List<BusStop>>

    data class BodyDriver(
        val token: String,
        val speed: Int
    )
    @POST("location/reportLocation/{latitude}/{longitude}")
    suspend fun postLatitudeLongitude(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Body bodyDriver: BodyDriver,
    ): Response<Unit>

    @POST("incidents/list/{idRoute}")
    suspend fun getIncidentsList(@Path("idRoute") idRoute: Int): Response<List<Incident>>
    data class IncidentRequest(
        val lat: Double,
        val lon: Double,
        val token: String,
        val description: String
    )
    @POST("incidents/add/{incidentType}/{idRoute}")
    suspend fun postIncident(
        @Path("incidentType") incident: String,
        @Path("idRoute") idRoute: Int,
        @Body incidentRequest: IncidentRequest
    ): Response<List<GenericResponse>>

    @POST("incidents/remove/{incidentId}")
    suspend fun deleteIncident(
        @Path("incidentId") incidentId: Int,
        @Body token: TokenRequest
    ): Response<List<GenericResponse>>

    @POST("location/getWaitTime/{idRoute}/{stopId}/")
    suspend fun getWaitTime(@Path("idRoute") idRoute: Int, @Path("stopId") stopId: Int): Response<List<InfoResponse>>
    data class StartTrip (
        val token: String,
        val routeId: Int
    )
    @POST("driver/startTrip")
    suspend fun startTrip(
        @Body token: StartTrip
    ): Response<GenericResponse>
    data class CancelTrip (
        val token: String,
        val reason: String
    )
    @POST("driver/cancelTrip")
    suspend fun cancelTrip(
        @Body token: CancelTrip
    ): Response<GenericResponse>

}