package com.example.kotlinmaps.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.Placeholder
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinmaps.databinding.RecyclerRowBinding
import com.example.kotlinmaps.model.Place
import com.example.kotlinmaps.view.MapsActivity


class PlaceAdapter(val placeList : List<Place>) : RecyclerView.Adapter<PlaceAdapter.PlaceHolder>() {



    class PlaceHolder(val recyclerRowBinding: RecyclerRowBinding) : RecyclerView.ViewHolder(recyclerRowBinding.root) {

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceHolder {
        val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PlaceHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return placeList.size
    }

    override fun onBindViewHolder(holder: PlaceHolder, position: Int) {
        holder.recyclerRowBinding.recyclerViewTextView.text = placeList.get(position).name
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, MapsActivity::class.java)
            //tiklandiqda save edilen bilgilerin gorunmesi
            intent.putExtra("selectedPlace", placeList.get(position))
            //tiklandiqda ustdeki menudan, yoxsa evvelden save edilmis yerden gedildiyini mueyyenlesdirmek
            intent.putExtra("info", "old")
            holder.itemView.context.startActivity(intent)
        }
    }


}