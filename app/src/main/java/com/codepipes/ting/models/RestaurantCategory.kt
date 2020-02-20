package com.codepipes.ting.models

class RestaurantCategory (
	val id: Int,
    val name: String,
    val country: String,
    val image: String,
    val createdAt: String,
    val updatedAt: String
){}

class CategoryRestaurant (
    val id: Int,
    val category: RestaurantCategory,
    val createdAt: String
) {}