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