package com.example.pasajero

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MyService : Service() {

    companion object {
        const val CHANNEL_ID = "MiServicioCanalID"
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalDeNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mi Aplicación")
            .setContentText("Ejecutándose en segundo plano")
            .setSmallIcon(R.drawable.bus_logo)
            .build()

        // Iniciar el servicio en primer plano
        startForeground(1, notification)

        // Aquí puedes ejecutar la tarea que deseas que el servicio realice
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Este servicio no está diseñado para ser enlazado a componentes, devuelve null
        return null
    }

    private fun crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Mi Servicio en Primer Plano",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
