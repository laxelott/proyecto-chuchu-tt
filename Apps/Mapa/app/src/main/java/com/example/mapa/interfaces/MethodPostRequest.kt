package com.example.mapa.interfaces


import com.example.mapa.BusStop
import com.example.mapa.UserInfo
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    /*
    * api/data/transport/list
    * api/data/route/list/idTransport
    * api/data/stop/list/idRoute
    * */
    @POST("/auth/authorize")
    suspend fun login(user: UserInfo): Response<Boolean>
    @POST("data/stop/list/{idRoute}")
    suspend fun getBusStopsInfo(@Path("idRoute") idRoute: Int): Response<List<BusStop>>
    @POST("location/register/{latitude}/{longitude}")
    suspend fun postLatitudeLongitude(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): Response<Unit>
}