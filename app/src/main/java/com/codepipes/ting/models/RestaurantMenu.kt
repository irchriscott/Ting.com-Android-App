package com.codepipes.ting.models

class RestaurantMenu (
    val id: Int,
    val type: MenuType,
    val urls: MenuUrls,
    val url: String,
    val menu: Menu
){}

class MenuType (
    val id: Int,
    val name: String
){}

class MenuUrls (
    val url: String,
    val like: String,
    val loadReviews: String,
    val addReview: String,
    val apiGet: String,
    val apiLike: String,
    val apiReviews: String
){}

class Menu (
    val id: Int,
    val restaurant: Restaurant?,
    val branch: Branch?,
    val name: String,
    val category: FoodCategory?,
    val dishTimeId: Int?,
    val dishTime: String?,
    val foodType: String?,
    val foodTypeId: Int?,
    val drinkTypeId: Int?,
    val drinkType: String?,
    val description: String,
    val ingredients: String,
    val showIngredients: Boolean,
    val price: Double,
    val lastPrice: Double,
    val currency: String,
    val isCountable: Boolean,
    val isAvailable: Boolean,
    val quantity: Int,
    val hasDrink: Boolean?,
    val drink: Menu?,
    val url: String,
    val promotions: MenuPromotions?,
    val reviews: MenuReviews?,
    val likes: MenuLikes?,
    val foods: MenuFoods?,
    val images: MenuImages,
    val createdAt: String,
    val updatedAt: String
){}

class MenuPromotions (
    val count: Int,
    val promotions: List<MenuPromotion>?
){}

class MenuReviews (
    val count: Int,
    val average: Float,
    val percents: List<Int>,
    val reviews: List<MenuReview>?
){}

class MenuLikes (
    val count: Int,
    val likes: List<MenuLike>?
){}

class MenuFoods (
    val count: Int,
    val foods: List<DishFood>?
){}

class DishFood(
   val id: Int,
   val food: Menu,
   val isCountable: Boolean,
   val quantity: Int,
   val createdAt: String,
   val updatedAt: String
){}

class MenuImage (
    val id: Int,
    val image: String,
    val createdAt: String
){}

class MenuImages (
    val count: Int,
    val images: List<MenuImage>
){}

