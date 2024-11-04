package com.example.pasajero

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class TravelFragment : Fragment(), InfoFragmentInterface {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.custom_travelling, container, false)
    }

    override fun updateData(newData: InfoResponse) {
        val nextStop: TextView = requireView().findViewById(R.id.traveling_nextStop_name)
        val nextStopTime: TextView = requireView().findViewById(R.id.traveling_nextStop_time)

        nextStop.text = newData.nextName
        nextStopTime.text = formatSecondsToTime(newData.nextTime)
    }

    private fun formatSecondsToTime(seconds: Float): String {
        val totalSeconds = seconds.toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return "${hours}h ${minutes}m ${secs}s"
    }

    fun dismissFragment() {
        // Dismiss the fragment
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }
}
