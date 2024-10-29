package com.example.mapa

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.Job

class EndTravelDialogFragment(private val sendingDataJob: Job?, private val gettingIncidentsJob: Job?) : DialogFragment() {

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
            endTravel()
        }
        btnClimaticEvent.setOnClickListener {
            endTravel()
        }
        btnTrafficEvent.setOnClickListener {
            endTravel()
        }
        btnHealthProblem.setOnClickListener {
            endTravel()
        }
        btnRoadClosed.setOnClickListener {
            endTravel()
        }

        builder.setView(view)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun endTravel() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Finalizar viaje")
            .setMessage("¿Quieres finalizar tu viaje?")
            .setPositiveButton("Si") { dialog, _ ->
                stopSendingDataPeriodically()
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
    }
}
