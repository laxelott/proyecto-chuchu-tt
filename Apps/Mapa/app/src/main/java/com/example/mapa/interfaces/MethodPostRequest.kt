package com.example.mapa.interfaces

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response

data class User(
    val name: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val errorMessage: String? = null
)

interface ApiService {
    suspend fun login(user: User): Response<LoginResponse>
}

