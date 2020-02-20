package com.codepipes.ting.models

class Booking (
    val id: Int,
    val branch: Branch?,
    val table: RestaurantTable?,
    val token: String,
    val people: Int,
    val date: String,
    val time: String,
    val isComplete: Boolean,
    val isCanceled: Boolean,
    val isAccepted: Boolean,
    val isRefunded: Boolean,
    val createdAt: String,
    val updatedAt: String
){}