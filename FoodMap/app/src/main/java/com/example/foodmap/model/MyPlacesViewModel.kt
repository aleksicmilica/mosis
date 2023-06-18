package com.example.foodmap.model

import androidx.lifecycle.ViewModel
import com.example.foodmap.data.MyPlace

class MyPlacesViewModel:ViewModel() {
    var  myPlacesList: ArrayList<MyPlace> = ArrayList<MyPlace>()

    fun addPlace(place:MyPlace){
        myPlacesList.add(place);

    }
    var selected:MyPlace?=null

}