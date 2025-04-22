package com.skyroute.example.model

import com.google.gson.annotations.SerializedName

data class RandomNames(

    @SerializedName("uid")
    val uid: String,

    @SerializedName("num_of_names")
    val numOfNames: Int,

    @SerializedName("names")
    val names: List<String>,
) {

    override fun toString(): String {
        return "RandomNames(uid='$uid', numOfNames=$numOfNames, names=[${names.joinToString()}])"
    }
}
