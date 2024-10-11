package com.example.pasajero.interfaces

import android.util.Log
import com.example.mapa.interfaces.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiHelper {

    /*
    * api/data/transport/list
    * api/data/route/list/idTransport
    * api/data/stop/list/idRoute
    * */
    // Prepare stm
    fun prepareApi(): ApiService {
        val baseUrl = "https://chuchu-backend-w3szgba2ra-vp.a.run.app/api/"
        val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        val apiService: ApiService by lazy {
            retrofit.create(ApiService::class.java)
        }
        return apiService
    }

    // Getting data from DB
    fun <T> getDataFromDB(serviceCall: suspend () -> Response<T>, processResponse: (Response<T>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<T> = serviceCall()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        processResponse(response)
                        Log.d("Response", "Datos recibidos correctamente")
                    } else {
                        Log.d("Response", "Error en la respuesta: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.d("Response", "Error de red o excepción: ${e.message}")
                }
            }
        }
    }


}