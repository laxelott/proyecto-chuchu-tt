package com.example.pasajero

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class InfoFragment : Fragment(), InfoFragmentInterface {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.custom_message, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismissFragment()
                true
            } else {
                false
            }
        }
    }

    override fun updateData(newData: InfoResponse) {
        val nextStop: TextView = requireView().findViewById(R.id.info_nextStop)
        val nextStopTime: TextView = requireView().findViewById(R.id.info_nextStop_time)
        val totalDistance: TextView = requireView().findViewById(R.id.info_total_distance)
        val totalTime: TextView = requireView().findViewById(R.id.info_total_distance_time)

        nextStop.text = newData.nextName
        nextStopTime.text = formatSecondsToTime(newData.nextTime)
        totalDistance.text = newData.totalDistance.toInt().toString() + " m"
        totalTime.text = formatSecondsToTime(newData.totalTime)
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
