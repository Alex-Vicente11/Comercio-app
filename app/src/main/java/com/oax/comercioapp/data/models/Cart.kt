package com.oax.comercioapp.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Cart(
    @SerializedName("id_cart")
    val idCart: Int,
    
    @SerializedName("id_user")
    val idUser: Int,
    
    @SerializedName("id_product")
    val idProduct: Int,
    
    @SerializedName("quantity")
    val quantity: Int,
    
    @SerializedName("added_date")
    val addedDate: String
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val addedDate: String
)

// Request para agregar al carrito
data class CartRequest(
    @SerializedName("id_user")
    val idUser: Int,

    @SerializedName("id_product")
    val idProduct: Int,

    @SerializedName("quantity")
    val quantity: Int
)

// Request para actualizar carrito
data class CartUpdateRequest(
    @SerializedName("quantity")
    val quantity: Int
)

data class CartResponse(
    @SerializedName("success")
    val success: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: Cart? = null
)