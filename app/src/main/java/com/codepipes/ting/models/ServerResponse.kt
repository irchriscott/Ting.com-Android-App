package com.codepipes.ting.models

class ServerResponse (
    val type: String,
    val message: String,
    val status: Int,
    val redirect: String?,
    val msgs: List<Any>?
){}