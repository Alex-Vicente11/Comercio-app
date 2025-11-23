package com.oax.comercioapp.data.models

import com.google.gson.annotations.SerializedName

// Guest User
data class GuestCreateRequest(
    @SerializedName("guest_id")
    val guestId: String
)

data class GuestCreateResponse(
    val success: Boolean,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    val message: String
)

// Authentication
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("guest_id")
    val guestId: String?
)

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("guest_id")
    val guestId: String?
)

data class AuthResponse(
    val success: Boolean,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("user_name")
    val userName: String?,
    val email: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    val token: String?,
    @SerializedName("cart_migrated")
    val cartMigrated: Boolean,
    @SerializedName("cart_items_count")
    val cartItemsCount: Int,
    val message: String
)

// Token Validation
data class ValidateTokenResponse(
    val success: Boolean,
    val valid: Boolean,
    @SerializedName("user_id")
    val userId: Int?,
    @SerializedName("user_name")
    val userName: String?,
    val email: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    val message: String
)

// User Profile
data class ProfileResponse(
    val success: Boolean,
    val user: UserProfile?,
    val message: String?
)

data class UserProfile(
    @SerializedName("id_user")
    val idUser: Int,
    @SerializedName("user_name")
    val userName: String,
    val email: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean,
    @SerializedName("created_at")
    val createdAt: String?
)

data class UpdateProfileRequest(
    @SerializedName("user_name")
    val userName: String
)

// Cart
data class AddToCartRequest(
    @SerializedName("id_product")
    val idProduct: Int,
    val quantity: Int
)

data class UpdateCartRequest(
    @SerializedName("id_product")
    val idProduct: Int,
    val quantity: Int
)

data class RemoveCartRequest(
    @SerializedName("id_product")
    val idProduct: Int
)

data class CartResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("cart_item")
    val cartItem: CartItemInfo?
)

data class CartItemInfo(
    @SerializedName("id_cart")
    val idCart: Int,
    @SerializedName("id_product")
    val idProduct: Int,
    val quantity: Int
)

// Cart Detailed
data class CartDetailedResponse(
    val success: Boolean,
    val items: List<CartItemDetailed>,
    val total: Double,
    @SerializedName("item_count")
    val itemsCount: Int
)

data class CartItemDetailed(
    @SerializedName("id_cart")
    val idCart: Int,
    val product: ProductInfo,
    val quantity: Int,
    val subtotal: Double,
    @SerializedName("added_date")
    val addedDate: String
)

data class ProductInfo(
    @SerializedName("id_product")
    val idProduct: Int,
    val product: String,
    val price: Double
)

// Cart Count
data class CartCountResponse(
    val success: Boolean,
    val count: Int,
    @SerializedName("unique_products")
    val uniqueProducts: Int
)

// Cart Check
data class CartCheckResponse(
    val success: Boolean,
    val exists: Boolean,
    @SerializedName("cart_item")
    val cartItem: CartItemInfo?
)

// Cart Merge
data class MergeCartRequest(
    @SerializedName("guest_id")
    val guestId: String
)

data class MergeCartResponse(
    val success: Boolean,
    @SerializedName("merged_items")
    val mergedItems: Int,
    val message: String
)

// Health Check
data class HealthCheckResponse(
    val status: String,
    val timestamp: String,
    val version: String,
    val code: Int?
)
