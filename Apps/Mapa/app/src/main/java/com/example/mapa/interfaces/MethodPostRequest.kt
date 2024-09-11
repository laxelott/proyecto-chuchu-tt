package com.example.mapa.interfaces


import com.example.mapa.UserInfo
import retrofit2.Response
import retrofit2.http.POST

interface ApiService {
    /*
    * api/data/transport/list
    * api/data/route/list/idTransport
    * api/data/stop/list/idRoute
    * */
    @POST("data/transport/list")
    suspend fun login(user: UserInfo): Response<UserInfo>

}