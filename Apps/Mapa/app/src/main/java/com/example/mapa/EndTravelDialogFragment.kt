package com.example.mapa

import android.app.Dialog
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class EndTravelDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        // Inflate the custom layout for the dialog
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_end_travel, null)
        builder.setView(view)

        // Get all buttons
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

        // Add action buttons if needed
        builder.setView(view)
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Close the dialog without ending the travel
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun endTravel() {
        // Logic to end the travel and close the dialog
        dismiss()
        requireActivity().finish()  // Or other logic to end the travel
    }
}