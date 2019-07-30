package com.codepipes.ting.models

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
    val tables: BranchTables,
    val specials: List<Int>,
    val menus: BranchMenus,
    val promotions: BranchPromotions?,
    val reviews: BranchReviews?,
    val likes: BranchLikes?,
    val urls: BranchUrls,
    val createdAt: String,
    val updatedAt: String
){
    init {
        this.dist = 0.00
    }
}

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
    val loadLikes: String
){}

