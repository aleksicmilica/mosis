package com.example.foodmap.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*

@SuppressLint("MissingPermission")
class LocationClient constructor(
    private val context: Context,
    private var iLocationClient: ILocationClient?
) {

    private val client = LocationServices.getFusedLocationProviderClient((context))
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.lastOrNull()?.let { location ->
                iLocationClient?.onNewLocation(location)
            }
        }
    }

    init {

        val request =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateDistanceMeters(1.0f)
                .build()


        client.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )


    }

    fun setILocationClient(iLocationClient: ILocationClient?){
        this.iLocationClient = iLocationClient
    }

    fun stop(){
        client.removeLocationUpdates(locationCallback)

    }


}