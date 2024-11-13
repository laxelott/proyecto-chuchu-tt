package com.example.mapa

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.mapa.interfaces.ApiService
import com.example.pasajero.interfaces.ApiHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReportIncidentDialogFragment(private val routeID: Int, private val lat: Double, private val lon: Double, private val token: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_report_incident, null)
        builder.setView(view)

        val btnVehicularCrash: LinearLayout = view.findViewById(R.id.btn_vehicular_crash)
        val btnTraffic: LinearLayout = view.findViewById(R.id.btn_traffic)
        val btnBottleneck: LinearLayout = view.findViewById(R.id.btn_bottleneck)
        val btnRoadClosed: LinearLayout = view.findViewById(R.id.btn_road_closed)
        val btnManifestation: LinearLayout = view.findViewById(R.id.btn_manifestation)
        val btnOther: LinearLayout = view.findViewById(R.id.btn_other)

        btnVehicularCrash.setOnClickListener {
            sendReportIncident("0")
        }
        btnTraffic.setOnClickListener {
            sendReportIncident("1")
        }
        btnBottleneck.setOnClickListener {
            sendReportIncident("2")
        }
        btnRoadClosed.setOnClickListener {
            sendReportIncident("3")
        }
        btnManifestation.setOnClickListener {
            sendReportIncident("4")
        }
        btnOther.setOnClickListener {
            sendReportIncident("5")
        }

        builder.setView(view)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun sendReportIncident(type: String) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Reportar incidencia")
            when (type) {
                "0" -> {
                    builder.setMessage("¿Quieres reportar la incidencia 'Choque vehicular'?")
                }
                "1" -> {
                    builder.setMessage("¿Quieres reportar la incidencia 'Tráfico'?")
                }
                "2" -> {
                    builder.setMessage("¿Quieres reportar la incidencia 'Embotellamiento'?")
                }
                "3" -> {
                    builder.setMessage("¿Quieres reportar la incidencia 'Cierre vial'?")
                }
                "4" -> {
                    builder.setMessage("¿Quieres reportar la incidencia 'Manifestación'?")
                }
                "5" -> {
                    builder.setMessage("¿Quieres reportar la incidencia 'Otro'?")
                }
                else -> {
                    builder.setMessage("Tipo de incidencia desconocido.")
                }
            }
            .setPositiveButton("Si") { dialog, _ ->
                callApi(type)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun callApi(type: String) {
        val incidentRequest = ApiService.IncidentRequest(lat, lon, token, "")
        val service = ApiHelper().prepareApi()

        ApiHelper().getDataFromDB(
            serviceCall = { service.postIncident(type, routeID, incidentRequest) },
            processResponse = { response ->
                CoroutineScope(Dispatchers.Main).launch { // Cambiar a hilo principal
                    if (response.isSuccessful && response.body() != null) {
                        // Asumimos que la respuesta es un arreglo JSON.
                        val listType = object : TypeToken<List<GenericResponse>>() {}.type

                        // Convertir el cuerpo de la respuesta directamente.
                        val incidentReported: List<GenericResponse> = Gson().fromJson(
                            Gson().toJson(response.body()),
                            listType
                        )

                        Log.d("Transport response", "Se reportó la incidencia")

                        // Procesar la respuesta basada en el primer elemento.
                        when (incidentReported.first().error) {
                            0 -> showToast("Incidente reportado")
                            1 -> showToast("Tipo de incidente inválido")
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

    // Función para mostrar Toast
    private fun showToast(message: String) {
        // Verifica que el Fragment esté adjunto antes de mostrar el Toast
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }


}