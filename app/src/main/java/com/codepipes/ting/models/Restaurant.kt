package com.codepipes.ting.models

class Restaurant (
    val id: Int,
    val token: String,
    val name: String,
    val motto: String,
    val purposeId: Int,
    val purpose: String,
    val categories: RestaurantCategories,
    val logo: String,
    val pin: String,
    val country: String,
    val town: String,
    val opening: String,
    val closing: String,
    val menus: BranchMenus,
    val branches: RestaurantBranches?,
    val images: RestaurantImages,
    val tables: BranchTables,
    val likes: BranchLikes,
    val foodCategories: RestaurantFoodCategories,
    val config: RestaurantConfig,
    val createdAt: String,
    val updatedAt: String
){}

class RestaurantConfig (
    val id: Int,
    val currency: String,
    val tax: Double,
    val email: String,
    val cancelLateBooking: Boolean,
    val bookWithAdvance: Boolean,
    val phone: String,
    val bookingCancelationRefund: Boolean,
    val bookingCancelationRefundPercent: Int,
    val daysBeforeReservation: Int,
    val canTakeAway: Boolean,
    val userShouldPayBefore: Boolean
){}

class RestaurantCategories (
    val count: Int,
    val categories: List<CategoryRestaurant>
){}

class RestaurantBranches (
    val count: Int,
    val branches: List<Branch>
){}

class RestaurantImage (
    val id: Int,
    val image: String,
    val createdAt: String
){}

class RestaurantImages (
    val count: Int,
    val images: List<RestaurantImage>
){}

class RestaurantFoodCategories (
    val count: Int,
    val foodCategories: List<FoodCategory>
){}