/*
 * Copyright (C) 2025 Andre Suryana, SkyRoute (https://github.com/skyroute)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skyroute.example.model

import com.google.gson.annotations.SerializedName

/**
 * A data class representing a random names object.
 *
 * @author Andre Suryana
 */
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
