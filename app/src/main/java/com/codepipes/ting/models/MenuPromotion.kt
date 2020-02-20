package com.codepipes.ting.models

class MenuPromotion (
    val id: Int,
    val restaurant: Restaurant?,
    val branch: Branch?,
    val occasionEvent: String,
    val uuid: String,
    val uuidUrl: String,
    val promotionItem: PromotionItem,
    val menus: PromotionMenus,
    val reduction: PromotionReduction,
    val supplement: PromotionSupplement,
    val period: String,
    val description: String,
    val posterImage: String,
    val isOn: Boolean,
    val isOnToday: Boolean,
    val interests: PromotionInterests,
    val urls: PromotionUrls,
    val createdAt: String,
    val updatedAt: String
){}

class PromotionItem (
    val type: PromotionItemType,
    val category: FoodCategory?,
    val menu: RestaurantMenu?
){}

class PromotionItemType (
    val id: Int,
    val name: String
){}

class PromotionReduction (
    val hasReduction: Boolean,
    val amount: Int,
    val reductionType: String
){}

class PromotionSupplement (
    val hasSupplement: Boolean,
    val minQuantity: Int,
    val isSame: Boolean,
    val supplement: RestaurantMenu?,
    val quantity: Int
){}

class PromotionInterest (
    val id: Int,
    val user: User,
    val isInterested: Boolean,
    val createdAt: String
){}

class PromotionInterests (
    val count: Int,
    val interests: List<Int>
){}

class PromotionUrls (
    val relative: String,
    val interest: String,
    val apiGet: String,
    val apiInterest: String
){}

class PromotionMenus(
    val count: Int,
    val menus: List<RestaurantMenu>?
){}