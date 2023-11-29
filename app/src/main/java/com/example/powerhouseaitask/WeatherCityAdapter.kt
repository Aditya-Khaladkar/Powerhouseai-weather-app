package com.example.powerhouseaitask

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WeatherCityAdapter(val list: List<WeatherCityModel>) : RecyclerView.Adapter<WeatherCityAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtExtraCity: TextView = itemView.findViewById(R.id.txtExtraCity)
        val txtExtraTemp: TextView = itemView.findViewById(R.id.txtExtraTemp)
        val txtExtraSky: TextView = itemView.findViewById(R.id.txtExtraSky)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.city_card_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtExtraCity.text = item.cityName
        holder.txtExtraTemp.text = "${item.temperature.toInt() - 273} °C"
        holder.txtExtraSky.text = item.weatherDescription
    }
}