package com.codepipes.ting.models

class SearchResult (
    val id: Int,
    val type: Int,
    val image: String,
    val name: String,
    val description: String,
    val text: String,
    val reviews: Int,
    val reviewAverage: Int,
    val apiGet: String,
    val relative: String
) {}