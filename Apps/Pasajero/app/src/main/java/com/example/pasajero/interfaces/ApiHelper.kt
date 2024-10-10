package com.example.pasajero.interfaces

import android.util.Log
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
    fun <T> getDataFromDB (serviceCall: suspend () -> Response<T>, processResponse: (Response<T>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<T> = serviceCall()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        processResponse(response)
                        // Handle successful login (e.g., navigate to next activity)
                        Log.d("Response", "Datos recibidos correctamente")
                    }
                } else {
                    // Handle error in login response (e.g., show error message)
                    Log.d("Response", "Error en la respuesta: ${response.code()}")
                }
            } catch (e: Exception) {
                // Handle network or other exceptions
                Log.d("Response", "Error de red o excepcion: ${e.message}")
            }
        }
    }

}