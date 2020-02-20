package com.codepipes.ting.models

class RestaurantTable (
    val id: Int,
    val branch: Branch?,
    val uuid: String,
    val maxPeople: Int,
    val number: String,
    val location: String,
    val chairType: String,
    val description: String,
    val isAvailable: Boolean,
    val createdAt: String,
    val updatedAt: String
){}