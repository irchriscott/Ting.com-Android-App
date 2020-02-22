package com.codepipes.ting.models

class Bill (
    val id: Int,
    val number: String,
    val token: String,
    val amount: Double?,
    val discount: Double?,
    val currency: String,
    val isRequested: Boolean,
    val isPaid: Boolean,
    val isComplete: Boolean,
    val paidBy: Int?,
    val orders: BillOrders?,
    val createdAt: String,
    val updatedAt: String
) {}


class BillOrders(
    val count: Int,
    val orders: List<Order>?
) {}


class Order () {}