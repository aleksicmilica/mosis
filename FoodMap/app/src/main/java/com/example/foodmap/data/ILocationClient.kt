package com.example.foodmap.data

import android.location.Location

interface ILocationClient {

    fun onNewLocation(location:Location)


}