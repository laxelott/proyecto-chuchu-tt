package com.example.pasajero.interfaces

import com.example.pasajero.BusStop
import com.example.pasajero.DriverLocation
import com.example.pasajero.Incident
import com.example.pasajero.InfoResponse
import com.example.pasajero.LineInfo
import com.example.pasajero.TransportInfo
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    /*
    * api/data/transport/list
    * api/data/route/list/idTransport
    * api/data/stop/list/idRoute
    * */
    @POST("data/transport/list")
    suspend fun postTransportList(): Response<List<TransportInfo>>
    @POST("data/route/list/{idTransport}")
    suspend fun postRoutesInfo(@Path("idTransport") idTransport: Int): Response<List<LineInfo>>
    @POST("data/stop/list/{idRoute}")
    suspend fun getBusStopsInfo(@Path("idRoute") idRoute: Int): Response<List<BusStop>>
    @POST("location/getLocations/{idRoute}")
    suspend fun getDriversLocations(@Path("idRoute") idRoute: Int): Response<List<DriverLocation>>
    @POST("incidents/list/{idRoute}")
    suspend fun getIncidentsList(@Path("idRoute") idRoute: Int): Response<List<Incident>>

    @POST("location/getWaitTimeForVehicle/{idRoute}/{stopId}/{identifier}")
    suspend fun getWaitTimeForVehicle(@Path("idRoute") idRoute: Int, @Path("stopId") stopId: Int, @Path("identifier") identifier: String): Response<List<InfoResponse>>
    @POST("location/getWaitTime/{idRoute}/{stopId}/")
    suspend fun getWaitTime(@Path("idRoute") idRoute: Int, @Path("stopId") stopId: Int): Response<List<InfoResponse>>
}

