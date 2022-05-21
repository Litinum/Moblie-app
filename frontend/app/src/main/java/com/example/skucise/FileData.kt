package com.example.skucise

import org.json.JSONObject

data class FileData(
    val Content: String,
    val Name: String,
    val Extension: String
)

fun FileDataToJson(fileData: FileData): JSONObject {
    val json = JSONObject()
    json.put("Content", fileData.Content)
    json.put("Name", fileData.Name)
    json.put("Extension", fileData.Extension)
    return json
}