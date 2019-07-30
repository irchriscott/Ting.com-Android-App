package com.codepipes.ting.models

class RestaurantReview (
    val id: Int,
    val user: User?,
    val restaurant: Restaurant?,
    val branch: Branch?,
    val review: Int,
    val comment: String,
    val createdAt: String,
    val updatedAt: String
){}