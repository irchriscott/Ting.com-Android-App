package com.codepipes.ting.models

import com.google.android.gms.maps.model.LatLng

class Branch (
    val id: Int,
    val restaurant: Restaurant?,
    val name: String,
    val country: String,
    val town: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    var dist: Double,
    val placeId: String,
    val email: String,
    val phone: String,
    val isAvailable: Boolean,
    val categories: RestaurantCategories,
    val tables: BranchTables,
    val specials: List<BranchSpecial>,
    val menus: BranchMenus,
    val promotions: BranchPromotions?,
    val reviews: BranchReviews?,
    val likes: BranchLikes?,
    val urls: BranchUrls,
    val createdAt: String,
    val updatedAt: String,
    var fromLocation: LatLng
){
    init {
        this.dist = 0.00
    }
}

class BranchSpecial(
    val id: Int,
    val name: String,
    val icon: String
){}

class BranchTables (
    val count: Int,
    val iron: Int,
    val wooden: Int,
    val plastic: Int,
    val couch: Int,
    val mixture: Int,
    val inside: Int,
    val outside: Int,
    val balcony: Int,
    val rooftop: Int,
    val tables: List<RestaurantTable>?
){}

class BranchMenus (
    val count: Int,
    val type: BranchMenusType,
    val menus: List<RestaurantMenu>?
){}

class BranchMenusType (
    val foods: BranchMenusTypeFood,
    val drinks: Int,
    val dishes: Int
){}

class BranchMenusTypeFood (
    val count: Int,
    val type: BranchMenusTypeFoodTypes
){}

class BranchMenusTypeFoodTypes (
    val appetizers: Int,
    val meals: Int,
    val desserts: Int,
    val sauces: Int
){}

class BranchPromotions (
    val count: Int,
    val promotions: List<MenuPromotion>
){}

class BranchReviews (
    val count: Int,
    val average: Float,
    val percents: List<Int>,
    val reviews: List<RestaurantReview>?
){}

class BranchLikes (
    val count: Int,
    val likes: List<UserRestaurant>?
){}

class BranchUrls (
    val relative: String,
    val loadReviews: String,
    val addReview: String,
    val likeBranch: String,
    val loadLikes: String,
    val apiGet: String,
    val apiPromotions: String,
    val apiFoods: String,
    val apiDrinks: String,
    val apiDishes: String,
    val apiReviews: String,
    val apiAddReview: String,
    val apiLikes: String,
	val apiAddLike: String
){}

