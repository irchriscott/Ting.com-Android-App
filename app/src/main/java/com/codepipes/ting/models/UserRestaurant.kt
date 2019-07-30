package com.codepipes.ting.models

class UserRestaurant (
    val id: Int,
    val user: User,
    val restaurant: Restaurant,
    val createdAt: String,
    val updatedAt: String
){}