package com.example.skucise

import org.json.JSONObject

class FilterArray(
    private val filters: ArrayList<JSONObject> = ArrayList()
) {
    enum class FilterNames {
        NumBedrooms,
        Price,
        City,
        SaleType,
        Size,
        NumBathrooms,
        StructureType,
        ResidenceType,
        Furnished
    }

    enum class SaleTypes {
        Purchase,
        Rent
    }

    fun applyFilter(name:  FilterNames, value1: Double, value2: Double) {
        val range = JSONObject()
        range.put("From", value1)
        range.put("To", value2)
        applyFilter(name, range)
    }

    fun applyFilter(name: FilterNames, value: Any) {
        val filter = JSONObject()
        filter.put("Name", name.toString())

        val validValue = when(name) {
            FilterNames.NumBedrooms -> value is Int
            FilterNames.Price -> value is JSONObject
            FilterNames.City -> value is ArrayList<*>
            FilterNames.SaleType -> value is Int
            FilterNames.Size -> value is JSONObject
            FilterNames.NumBathrooms -> value is Int
            FilterNames.StructureType -> value is ArrayList<*>
            FilterNames.ResidenceType -> value is Int
            FilterNames.Furnished -> value is String
        }

        if (validValue) {
            filter.put("Param", value)
            filters.add(filter)
        }
        else {
            throw Exception("ERROR :: FilterArray.applyFilter :: Param value impossible")
        }
    }

    fun getFilters() : String {
        return filters.toString()
    }
}