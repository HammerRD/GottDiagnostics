package com.gottdiagnostics

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Stable IDs keep photos, notes and recordings separate even for identical models. */
object Garage {
    fun create(year: String, make: String, model: String, paint: String, details: String): Vehicle {
        require(year.toIntOrNull() in 1886..(java.time.Year.now().value + 2)) { "Enter a valid four-digit model year." }
        require(make.trim().isNotEmpty() && model.trim().isNotEmpty()) { "Make and model are required." }
        require(make.length <= 60 && model.length <= 100 && paint.length <= 40 && details.length <= 4000) { "A profile field is too long." }
        return Vehicle("custom-" + UUID.randomUUID(), "${year.trim()} ${make.trim()} ${model.trim()}",
            details.trim().ifBlank { "Engine, transmission, fuel, modifications and tune: not specified." },
            make.trim(), paint.trim().ifBlank { "PAINT NOT SET" })
    }
    fun encode(vehicles: List<Vehicle>): String = JSONArray().apply {
        vehicles.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("details", it.details).put("make", it.make).put("paint", it.paint)) }
    }.toString()
    fun decode(json: String): List<Vehicle> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                val id = item.getString("id")
                require(id.startsWith("custom-"))
                UUID.fromString(id.removePrefix("custom-"))
                val title = item.getString("title"); val make = item.getString("make")
                require(title.isNotBlank() && make.isNotBlank())
                Vehicle(id, title, item.getString("details"), make, item.optString("paint", "PAINT NOT SET"))
            }.getOrNull()
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())
}

internal fun Vehicle.profile(notes: String) = "$title\nPaint: $paint\n$details\nOwner notes / symptoms: $notes"

/** Legacy logs have a profile snapshot; new logs also carry the stable garage ID. */
internal fun sessionMatchesVehicle(lines: Sequence<String>, vehicle: Vehicle): Boolean {
    val headers = lines.take(5).mapNotNull { runCatching { JSONObject(it) }.getOrNull() }.toList()
    val id = headers.firstOrNull { it.optString("type") == "vehicle_id" }?.optString("data")
    if(id != null) return id == vehicle.id
    return !vehicle.id.startsWith("custom-") && headers.any {
        it.optString("type") == "vehicle" && it.optString("data").substringBefore('\n') == vehicle.title
    }
}
