package com.codepipes.ting.models

class Booking (
    val id: Int,
    val branch: Branch?,
    val table: RestaurantTable?,
    val token: String,
    val people: Int,
    val date: String,
    val time: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
){}