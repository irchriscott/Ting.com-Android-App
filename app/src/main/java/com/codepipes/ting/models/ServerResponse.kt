package com.codepipes.ting.models

class ServerResponse (
    val type: String,
    val message: String,
    val status: Int,
    val redirect: String?,
    val user: User?,
    val msgs: List<Any>?
){}