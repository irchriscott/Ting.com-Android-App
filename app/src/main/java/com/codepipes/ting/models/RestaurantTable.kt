package com.codepipes.ting.models

class RestaurantTable (
    val id: Int,
    val uuid: String,
    val maxPeople: Int,
    val number: Int,
    val location: String,
    val chairType: String,
    val description: String,
    val isAvailable: Boolean,
    val createdAt: String,
    val updatedAt: String
){}