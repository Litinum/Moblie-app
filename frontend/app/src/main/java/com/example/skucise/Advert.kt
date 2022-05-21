package com.example.skucise

import android.annotation.SuppressLint
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

data class Advert(
    val id: UInt,
    val residenceType: ResidenceType = ResidenceType.Stan,
    val saleType: SaleType,
    val structureType: StructureType = StructureType.Garsonjera,
    val title: String,
    val description: String = "",
    val city: String,
    val address: String,
    val size: Double,
    val price: Double,
    val ownerId: UInt = 0u,
    val numberOfBedrooms: UInt = 0u,
    val numberOfBathrooms: UInt = 0u,
    val yearOfMake: UInt = 0u,
    val furnished: Boolean = false,
    val dateCreated: LocalDateTime,
    val images: ArrayList<String> = ArrayList()
)

@SuppressLint("NewApi")
fun loadAdverts(jsonArray: JSONArray): ArrayList<Advert> {
    val adverts = ArrayList<Advert>()
    for (i in 0 until jsonArray.length()) {
        val json = jsonArray[i] as JSONObject
        // load images
        val imagesJson = json.getJSONArray("images")
        val images = ArrayList<String>()
        for (j in 0 until imagesJson.length())
            images.add(imagesJson[j] as String)

        adverts.add( Advert(
            id            = json.getInt("id").toUInt(),
            ownerId       = json.getInt("ownerId").toUInt(),
            title         = json.getString("title"),
            price         = json.getDouble("price"),
            city          = json.getString("city"),
            address       = json.getString("address"),
            saleType      = SaleType.values()[json.getInt("saleType")],
            residenceType = ResidenceType.values()[json.getInt("residenceType")],
            size          = json.getDouble("size"),
            dateCreated   = LocalDateTime.parse(json.getString("dateCreated")),
            images        = images
        ))
    }
    return adverts
}

enum class ResidenceType { Stan, Kuća }
enum class StructureType { Garsonjera, Jednosobna, JednaIpoSoba, Dvosobna, DveIpoSobe }
enum class SaleType { Prodaja, Iznajmljivanje }