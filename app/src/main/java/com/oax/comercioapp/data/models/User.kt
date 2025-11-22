package com.oax.comercioapp.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id_user")
    val idUser: Int,
    
    @SerializedName("user_name")
    val userName: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("is_guest")
    val isGuest: Boolean = false,

    @SerializedName("token")
    val token: String? = null
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