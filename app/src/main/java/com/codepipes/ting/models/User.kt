package com.codepipes.ting.models

import com.codepipes.ting.utils.Routes

class User (
    val id: Int,
    val token: String,
    val name: String,
    val username: String,
    val email: String,
    private val image: String,
    private val pin: String,
    val pinImg: String,
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
){
    public fun imageURL(): String = "${Routes().UPLOAD_END_POINT}${this.image}"
    public fun pinURL(): String = "${Routes().HOST}${this.pin}"
}

class Address (
    val id: Int,
    var type: String,
    var address: String,
    var latitude: Double,
    var longitude: Double,
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