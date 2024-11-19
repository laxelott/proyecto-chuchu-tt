package com.example.mapa

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.mapa.interfaces.ApiService
import com.example.pasajero.interfaces.ApiHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EndTravelDialogFragment(private val sendingDataJob: Job?, private val gettingIncidentsJob: Job?, private val gettingInfoJob: Job?, private val token: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_end_travel, null)
        builder.setView(view)

        val btnUnitFailure: LinearLayout = view.findViewById(R.id.btn_unit_failure)
        val btnClimaticEvent: LinearLayout = view.findViewById(R.id.btn_climatic_event)
        val btnTrafficEvent: LinearLayout = view.findViewById(R.id.btn_traffic_accident)
        val btnHealthProblem: LinearLayout = view.findViewById(R.id.btn_health_problem)
        val btnRoadClosed: LinearLayout = view.findViewById(R.id.btn_road_closed)

        btnUnitFailure.setOnClickListener {
            endTravel("Fallo en la unidad")
        }
        btnClimaticEvent.setOnClickListener {
            endTravel("Evento climatico")
        }
        btnTrafficEvent.setOnClickListener {
            endTravel("Accidente de trafico")
        }
        btnHealthProblem.setOnClickListener {
            endTravel("Problema de salud")
        }
        btnRoadClosed.setOnClickListener {
            endTravel("Vialidad cerrada")
        }

        builder.setView(view)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun endTravel(reason: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Finalizar viaje")
            .setMessage("¿Quieres finalizar tu viaje?")
            .setPositiveButton("Si") { dialog, _ ->
                stopSendingDataPeriodically()
                (activity as? MapaActivity)?.leaveVehicle()
                callApi(reason)
                dismiss()
                requireActivity().finish()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun stopSendingDataPeriodically() {
        sendingDataJob?.cancel()
        gettingIncidentsJob?.cancel()
        gettingInfoJob?.cancel()
    }

    private fun callApi(reason: String) {
        val cancelReason = ApiService.CancelTrip(token, reason)
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.cancelTrip(cancelReason)},
            processResponse = { response ->
                CoroutineScope(Dispatchers.Main).launch {
                    val responseBody = response.body()
                    if(responseBody != null) {
                        when (responseBody.error) {
                            0 -> {}
                            2 -> showToast("Token inválida")
                            else -> showToast("Error desconocido")
                        }
                    } else {
                        showToast("Error en la respuesta: ${response.code()}")
                    }
                    dismiss() // Mover dismiss aquí
                }
            }
        )
    }

    private fun showToast(message: String) {
        // Verifica que el Fragment esté adjunto antes de mostrar el Toast
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}
