package com.example.mapa

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.pasajero.interfaces.ApiHelper

class ReportIncidentDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        // Inflate the custom layout for the dialog
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_report_incident, null)
        builder.setView(view)

        // Get all buttons
        val btnVehicularCrash: LinearLayout = view.findViewById(R.id.btn_vehicular_crash)
        val btnTraffic: LinearLayout = view.findViewById(R.id.btn_traffic)
        val btnBottleneck: LinearLayout = view.findViewById(R.id.btn_bottleneck)
        val btnRoadClosed: LinearLayout = view.findViewById(R.id.btn_road_closed)
        val btnManifestation: LinearLayout = view.findViewById(R.id.btn_manifestation)
        val btnOther: LinearLayout = view.findViewById(R.id.btn_other)

        btnVehicularCrash.setOnClickListener {
            sendReportIncident("Choque")
        }
        btnTraffic.setOnClickListener {
            sendReportIncident("Trafico")
        }
        btnBottleneck.setOnClickListener {
            sendReportIncident("Embotellamiento")
        }
        btnRoadClosed.setOnClickListener {
            sendReportIncident("Cerrado")
        }
        btnManifestation.setOnClickListener {
            sendReportIncident("Manifestacion")
        }
        btnOther.setOnClickListener {
            sendReportIncident("Otro")
        }

        builder.setView(view)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun sendReportIncident(type: String) {
        Toast.makeText(requireContext(), "Incidencia reportada: $type", Toast.LENGTH_SHORT).show()
        //callApi()
        dismiss()
    }

    private fun callApi() {
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.postIncident(60001) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val incidentReported = response.body()
                if (incidentReported != null) {
                    Log.d("Transport response", "Se reporto la incidencia: $incidentReported")
//                    AQUI DEBE DE IR EL TOAST
                }
            }
        )
    }
}