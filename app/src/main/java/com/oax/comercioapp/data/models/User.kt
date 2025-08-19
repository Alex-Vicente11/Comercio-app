package com.oax.comercioapp.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id_user")
    val idUser: Int,
    
    @SerializedName("user_name")
    val userName: String
)

data class UserRequest(
    @SerializedName("user_name")
    val userName: String
)

data class UserResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("id")
    val id: Int? = null,
    
    @SerializedName("user_name")
    val userName: String? = null
)