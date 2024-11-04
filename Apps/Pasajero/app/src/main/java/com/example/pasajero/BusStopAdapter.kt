package com.example.pasajero

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pasajero.databinding.ActivityMapsBinding
import com.example.pasajeropackage.MarkerTag
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class BusStopAdapter(
    private var listaStations: List<BusStop>,
    private var mMap: GoogleMap,
    private var binding: ActivityMapsBinding,
    private var busStopMarkers: MutableMap<BusStop, Marker>
): RecyclerView.Adapter<BusStopAdapter.ViewHolder>() {
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvNombre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_bus_stop, parent, false)
        return ViewHolder(vista)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val busStop = listaStations[position]
        holder.tvName.text = busStop.name
        holder.itemView.setOnClickListener {
            val imm = binding.searchBar.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val searchBarFocusStatus = binding.searchBar.findFocus()
            if (searchBarFocusStatus != null){
                imm.hideSoftInputFromWindow(searchBarFocusStatus.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                binding.searchBar.clearFocus()
                binding.searchBar.setText("")
            }
            binding.rvEstaciones.visibility = View.GONE
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(busStop.latitude, busStop.longitude), 18f))
            // Buscar el marcador correspondiente y mostrar el InfoWindow
            val marker = findMarkerForBusStop(busStop)
            if (marker != null) {
                if (marker.tag is MarkerTag) {
                    (marker.tag as MarkerTag).type = "busStop"
                    (marker.tag as MarkerTag).mode = InfoMode.TARGETED

                }
            }
            marker?.showInfoWindow()
        }
    }

    override fun getItemCount(): Int {
        return listaStations.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filtrar(listaFiltrada: ArrayList<BusStop>){
        this.listaStations = listaFiltrada
        notifyDataSetChanged()
    }

    private fun findMarkerForBusStop(busStop: BusStop): Marker? {
        return busStopMarkers[busStop]
    }

}