package com.codepipes.ting.models

class ServerResponse (
    val type: String,
    val message: String,
    val status: Int,
    val redirect: String?,
    val user: User?,
    val msgs: List<Any>?
){}

class MapPin(
    val id: Int,
    val pin: String
){}