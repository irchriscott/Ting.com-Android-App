package com.codepipes.ting.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

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
    val orders: List<OrderData>?
) {}


class Order (
    val id: Int,
    val menu: RestaurantMenu,
    val token: String,
    val quantity: Int,
    val price: Double,
    val currency: String,
    val conditions: String?,
    val isAccepted: Boolean,
    val isDeclined: Boolean,
    val reasons: String?,
    val hasPromotion: Boolean,
    val promotion: PromotionDataString?,
    val createdAt: String,
    val updatedAt: String
) {}


class OrderData (
    val id: Int,
    val menu: String,
    val token: String,
    val quantity: Int,
    val price: Double,
    val currency: String,
    val conditions: String?,
    val isAccepted: Boolean,
    val isDeclined: Boolean,
    val reasons: String?,
    val hasPromotion: Boolean,
    val promotion: PromotionDataString?,
    val createdAt: String,
    val updatedAt: String
) {
    val total: Double
        get() = quantity * price
}