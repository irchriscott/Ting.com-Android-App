package com.codepipes.ting.models

class User (
    val id: Int,
    val token: String,
    val name: String,
    val username: String,
    val email: String,
    val image: String,
    val pin: String,
    val phone: String,
    val dob: String?,
    val gender: String?,
    val country: String,
    val town: String,
    val restaurants: UserRestaurants?,
    val reviews: UserRestaurantReviews?,
    val addresses: UserAddresses?,
    val urls: UserUrls,
    val createdAt: String,
    val updatedAt: String
){}

class Address (
    val id: Int,
    val type: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String,
    val updatedAt: String
){}

class UserRestaurants (
    val count: Int,
    val restaurants: List<UserRestaurant>?
){}

class UserAddresses (
    val count: Int,
    val addresses: List<Address>
){}

class UserUrls (
    val loadRestaurants: String,
    val loadReservations: String
){}

class UserRestaurantReviews (
    val count: Int,
    val reviews: List<RestaurantReview>?
){}