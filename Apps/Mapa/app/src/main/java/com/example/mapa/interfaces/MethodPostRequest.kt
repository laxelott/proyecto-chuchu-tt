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
    @POST("data/transport/list")
    suspend fun login(user: UserInfo): Response<UserInfo>
    @POST("data/stop/list/{idRoute}")
    suspend fun getBusStopsInfo(@Path("idRoute") idRoute: Int): Response<List<BusStop>>
}