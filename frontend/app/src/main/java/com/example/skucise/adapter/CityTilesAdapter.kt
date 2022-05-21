package com.example.skucise.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.skucise.CityImageMap
import com.example.skucise.FilterArray
import com.example.skucise.R
import com.example.skucise.TileSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.item_city_tile.view.*

class CityTilesAdapter(private val tiles: List<TileSet>, private val navigationView: BottomNavigationView)
    : RecyclerView.Adapter<CityTilesAdapter.CityTilesViewHolder>() {

    class CityTilesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private val img = CityImageMap()
    lateinit var parent : ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityTilesViewHolder {
        this.parent = parent
        return CityTilesViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_city_tile,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CityTilesViewHolder, position: Int) {
        val tile = tiles[position]
        holder.itemView.apply {
            //Toast.makeText(context, tiles[2].name1.lowercase().replace(' ', '_'), Toast.LENGTH_SHORT).show()

            tile_layout1.tv_tile1.text = tile.name1
            tile_layout1.imageView1.setImageResource(img.images.getValue(tile.name1))
            tile_layout1.imageView1.setOnClickListener { sendRequestForSearch(tile.name1) }

            tile_layout2.tv_tile2.text = tile.name2
            tile_layout2.imageView2.setImageResource(img.images.getValue(tile.name2))
            tile_layout2.imageView2.setOnClickListener { sendRequestForSearch(tile.name2) }

            tile_layout3.tv_tile3.text = tile.name3
            tile_layout3.imageView3.setImageResource(img.images.getValue(tile.name3))
            tile_layout3.imageView3.setOnClickListener { sendRequestForSearch(tile.name3) }
        }
    }

    override fun getItemCount(): Int {
        return tiles.size
    }

    private fun sendRequestForSearch(city : String) {
        val filters = FilterArray()
        filters.applyFilter(FilterArray.FilterNames.City, arrayListOf(city))

        val args = Bundle()
        args.putString("advertsFilterArray", filters.getFilters())
        parent.findNavController().navigate(navigationView.menu[1].itemId, args)
    }
}