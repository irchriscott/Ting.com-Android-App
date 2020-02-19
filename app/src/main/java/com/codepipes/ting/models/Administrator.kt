package com.codepipes.ting.models

class Administrator (
    val id: Int,
    val name: String,
    val username: String,
    val type: String,
    val email: String,
    val phone: String,
    val image: String,
    val badgeNumber: String,
    val channel: String,
    val permissions: List<String>,
    val createdAt: String,
    val updatedAt: String
){}
