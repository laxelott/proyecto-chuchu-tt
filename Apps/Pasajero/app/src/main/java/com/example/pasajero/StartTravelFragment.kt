package com.example.pasajero

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.Marker

class StartTravelFragment : Fragment(), InfoFragmentInterface {

    private lateinit var startButton: Button
    private var busStopOrigin: BusStop? = null
    private var driverIdentifier: String = "";
    private var busStop: BusStop? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.custom_start_travel, container, false)
        startButton = view.findViewById(R.id.start_travel_button)

        startButton.setOnClickListener {
            busStopOrigin?.let {
                // Call the function in the Activity to start the travel
                (activity as? MapsActivity)?.onStartTravelClicked(driverIdentifier)
            }
        }
        return view
    }

    fun setBusStop(newBusStop: BusStop, ) {
        busStop = newBusStop 
    }

    override fun updateData(newData: InfoResponse) {
        driverIdentifier = newData.identifier
        Log.d("info", "EN EL FRAGMENTO $newData")

        val closestBusStop: TextView = requireView().findViewById(R.id.start_travel_closest_stop)
        val vehicle: TextView = requireView().findViewById(R.id.start_travel_vehicle)
        val time: TextView = requireView().findViewById(R.id.start_travel_time)

        closestBusStop.text = busStop!!.name
        vehicle.text = newData.identifier
        time.text = formatSecondsToTime(newData.totalTime)
    }

    fun enableStartButton() {
        startButton.isEnabled = true
        startButton.alpha = 1f // Fully opaque
    }

    fun disableStartButton() {
        startButton.isEnabled = false
        startButton.alpha = 0.5f // Semi-transparent
    }

    private fun formatSecondsToTime(seconds: Float): String {
        val totalSeconds = seconds.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return "${hours}h ${minutes}m ${secs}s"
    }

    fun dismissFragment() {
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }
}
