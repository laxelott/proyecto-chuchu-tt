package com.example.pasajero

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class LineInfoAdapter(
    private var listaLineas: List<LineInfo> = listOf()
): RecyclerView.Adapter<LineInfoAdapter.ViewHolder>() {
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescripcion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_lines_info, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Check if listaLineas is not null
        if (listaLineas.isEmpty()) return

        val linea = listaLineas[position] // Access data safely
        holder.tvName.text = linea.name
        holder.tvDescription.text = linea.description

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, MapsActivity::class.java)
            intent.putExtra("routeID", linea.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int  {
        return listaLineas.size
    }

}