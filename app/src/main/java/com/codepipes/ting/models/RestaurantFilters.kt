package com.codepipes.ting.models

class RestaurantFilters (
    val availability: List<Filter>,
    val cuisines: List<Filter>,
    val services: List<Filter>,
    val specials: List<Filter>,
    val types: List<Filter>,
    val ratings: List<Filter>
) {}

class Filter(
    val id: Int,
    val title: String
) {}

class FiltersParameters(
    var availability: List<Int>,
    var cuisines: List<Int>,
    var services: List<Int>,
    var specials: List<Int>,
    var types: List<Int>,
    var ratings: List<Int>
) {}