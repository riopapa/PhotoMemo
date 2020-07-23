package com.nearbyplacepicker.model


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SimplePlace(
    @Json(name = "geometry")
    val geometry: Geometry,
    @Json(name = "name")
    val name: String = "",
    @Json(name = "photos")
    val photos: List<Photo> = emptyList(),
    @Json(name = "place_id")
    val placeId: String,
    @Json(name = "types")
    val types: List<String> = emptyList(),
    @Json(name = "vicinity")
    val vicinity: String = "",
    @Json(name = "formatted_address")
    val formattedAddress: String = ""
)