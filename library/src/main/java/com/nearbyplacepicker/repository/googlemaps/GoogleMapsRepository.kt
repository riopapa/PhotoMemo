package com.nearbyplacepicker.repository.googlemaps

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.nearbyplacepicker.Config
import com.nearbyplacepicker.NearByPlacePicker
import com.nearbyplacepicker.model.SearchResult
import com.nearbyplacepicker.model.SimplePlace
import com.nearbyplacepicker.repository.PlaceRepository
import io.reactivex.Single
import java.util.*


class GoogleMapsRepository constructor(
    private val googleClient: PlacesClient,
    private val googleMapsAPI: GoogleMapsAPI
) : PlaceRepository {


    /**
     * Finds all nearby places ranked by likelihood of being the place where the device is.
     *
     * This call will be charged according to
     * [Places SDK for Android Usage and
       Billing](https://developers.google.com/places/android-sdk/usage-and-billing#find-current-place)
     */
    @SuppressLint("MissingPermission")
    override fun getNearbyPlaces(): Single<List<Place>> {

        // Create request
        val request = FindCurrentPlaceRequest.builder(getPlaceFields()).build()

        return Single.create { emitter ->
            googleClient.findCurrentPlace(request).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let {
                        val placeList = sortByLikelihood(it.placeLikelihoods)
                        emitter.onSuccess(placeList.map { likelihood -> likelihood.place })
                    }
                    // Empty result
                    emitter.onSuccess(listOf())
                } else {
                    emitter.tryOnError(task.exception ?: Exception("No places for you..."))
                }
            }
        }
    }

    /** Finds all nearby places ranked by distance from the requested location.
     *
     * This call will be charged according to
     * [Places SDK WEB API Usage and
    Billing](https://developers.google.com/maps/billing/understanding-cost-of-use#nearby-search)
     */
    override fun getNearbyPlaces(location: LatLng): Single<List<Place>> {

        val locationParam = "${location.latitude},${location.longitude}"

        return googleMapsAPI.searchNearby(locationParam,NearByPlacePicker.mapsApiKey)
            .map { searchResult ->
                val placeList = mutableListOf<CustomPlace>()
                for (simplePlace in searchResult.results) {
                    placeList.add(mapToCustomPlace(simplePlace))
                }
                placeList
            }
    }

    /**
     * Fetches a photo for the place.
     *
     * This call will be charged according to
     * [Places SDK for Android Usage and
       Billing](https://developers.google.com/places/android-sdk/usage-and-billing#places-photo)
     */
    override fun getPlacePhoto(photoMetadata: PhotoMetadata): Single<Bitmap> {

        // Create a FetchPhotoRequest.
        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(Config.PLACE_IMG_WIDTH)
            .setMaxHeight(Config.PLACE_IMG_HEIGHT)
            .build()

        return Single.create { emitter ->
            googleClient.fetchPhoto(photoRequest).addOnSuccessListener {
                val bitmap = it.bitmap
                emitter.onSuccess(bitmap)
            }.addOnFailureListener {
                emitter.tryOnError(it)
            }
        }
    }

    /**
     * Uses Google Maps GeoLocation API to retrieve a place by its latitude and longitude.
     * This call will be charged according to
     * [Places SDK for Android Usage and
       Billing](https://developers.google.com/maps/documentation/geocoding/usage-and-billing#pricing-for-the-geocoding-api)
     */
    override fun getPlaceByLocation(location: LatLng): Single<Place> {

        val paramLocation = "${location.latitude},${location.longitude}"

        return googleMapsAPI.findByLocation(paramLocation, NearByPlacePicker.mapsApiKey)
            .map { result: SearchResult ->
                if (("OK" == result.status) && result.results.isNotEmpty()) {
                    return@map mapToCustomPlace(result.results[0])
                }
                return@map PlaceFromCoordinates(location.latitude, location.longitude)
            }
    }

    /**
     * These fields are not charged by Google.
     * https://developers.google.com/places/android-sdk/usage-and-billing#basic-data
     */
    private fun getPlaceFields(): List<Place.Field> {

        return listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.TYPES,
            Place.Field.PHOTO_METADATAS
        )
    }

    private fun mapToCustomPlace(place: SimplePlace): CustomPlace {

        val photoList = mutableListOf<PhotoMetadata>()
        place.photos.forEach {
            val photoMetadata = PhotoMetadata.builder(it.photoReference)
                .setAttributions(it.htmlAttributions.toString())
                .setHeight(it.height)
                .setWidth(it.width)
                .build()
            photoList.add(photoMetadata)
        }

        val typeList = mutableListOf<Place.Type>()
        place.types.forEach { simpleType ->
            val placeType = Place.Type.values()
                .find { it.name == simpleType.toUpperCase(Locale.getDefault()) } ?: Place.Type.OTHER
            typeList.add(placeType)
        }

        val latLng = LatLng(place.geometry.location.lat, place.geometry.location.lng)

        val address: String =
            if (place.formattedAddress.isNotEmpty()) place.formattedAddress
            else place.vicinity

        val name: String = buildPlaceName(place.name, address)

         return CustomPlace(place.placeId, name, photoList, address, typeList, latLng)
    }

    private fun buildPlaceName(originalName: String, address: String): String {
        // We have a nice name, use it
        if (originalName.isNotEmpty()) {
            return originalName
        }
        // Return the first part of the address, usually the street + number
        return address.split(",").first()
    }

    /**
     * Sorts the list by Likelihood. The best ranked places come first.
     */
    private fun sortByLikelihood(placeLikelihoods: List<PlaceLikelihood>): List<PlaceLikelihood> {

        val mutableList = placeLikelihoods.toMutableList()

        mutableList.sortByDescending { it.likelihood }

        return mutableList
    }
}