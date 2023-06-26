package com.example.foodmap.data

import java.util.*


data class MyPlace(
    var name:String,
    var latitude:String,
    var longitude:String,
    var autor:String,
    var tip:String,
    var createdAt: Date?,
    var url:String,
  //  var description: String,
    var grades:HashMap<String,Double>,
    var comments:HashMap<String,String>,
    @Transient var id:String){



    override fun toString(): String {
           return name
    }
    fun addGrade(username:String,grade:Double){

        if(grades==null)
            grades = HashMap<String,Double>()
        grades[username] = grade
    }
    fun  addComment(username: String,comm:String){
        if(comments == null)
            comments = HashMap<String,String>()
        comments[username] = comm
    }

}
