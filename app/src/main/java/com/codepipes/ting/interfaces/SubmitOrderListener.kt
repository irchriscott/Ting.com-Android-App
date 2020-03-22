package com.codepipes.ting.interfaces

interface SubmitOrderListener {
    fun onSubmitOrder(quantity: String, conditions: String)
}