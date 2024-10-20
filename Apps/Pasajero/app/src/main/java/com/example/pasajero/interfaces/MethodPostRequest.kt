package com.example.pasajero.interfaces

import com.example.pasajero.BusStop
import com.example.pasajero.ConductorLocation
import com.example.pasajero.DriverLocation
import com.example.pasajero.LineInfo
import com.example.pasajero.TransportInfo
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
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
    @POST("incident/list/{idRoute}")
    suspend fun getIncidentsList(@Path("idRoute") idRoute: Int): Response<Boolean>
}

