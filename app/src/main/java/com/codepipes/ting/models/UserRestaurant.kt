package com.codepipes.ting.models

class UserRestaurant (
    val id: Int,
    val user: User,
    val branch: Branch,
    val createdAt: String,
    val updatedAt: String
){}