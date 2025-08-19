package com.oax.comercioapp.data.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class Product(
    @SerializedName("id_product")
    val idProduct: Int,
    
    @SerializedName("product")
    val product: String,
    
    @SerializedName("price")
    val price: Double
)

data class ProductRequest(
    @SerializedName("product")
    val product: String,
    
    @SerializedName("price")
    val price: Double
)

data class ProductUpdateRequest(
    @SerializedName("product")
    val product: String? = null,
    
    @SerializedName("price")
    val price: Double? = null
)

data class ProductResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("id")
    val id: Int? = null
)