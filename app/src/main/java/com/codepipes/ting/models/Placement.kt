package com.codepipes.ting.models

class Placement (
    val id: Int,
    val user: User,
    val table: RestaurantTable,
    val booking: Booking?,
    val waiter: Administrator?,
    val token: String,
    val people: Int,
    val isDone: Boolean,
    val needSomeone: Boolean,
    val createdAt: String,
    val updatedAt: String
) {}