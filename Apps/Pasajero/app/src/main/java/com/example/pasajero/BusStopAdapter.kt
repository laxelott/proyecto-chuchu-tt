package com.example.pasajero

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.app.PendingIntentCompat.getActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.example.pasajero.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class BusStopAdapter(
    var listaStations: ArrayList<BusStop> = ArrayList() ,
    var mMap: GoogleMap,
    var binding: ActivityMapsBinding
): RecyclerView.Adapter<BusStopAdapter.ViewHolder>() {
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvName = itemView.findViewById(R.id.tvNombre) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusStopAdapter.ViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_bus_stop, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: BusStopAdapter.ViewHolder, position: Int) {
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
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(busStop.latitude, busStop.longitude), 16f))
        }
    }

    override fun getItemCount(): Int {
        return listaStations.size ?: 0
    }

    fun filtrar(listaFiltrada: ArrayList<BusStop>){
        this.listaStations = listaFiltrada
        notifyDataSetChanged()
    }
}