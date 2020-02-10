package com.codepipes.ting.utils


class Routes {

    public val HOST_END_POINT: String       = "http://10.0.2.2:8000"
    private val END_POINT: String           = "${HOST_END_POINT}/api/v1/"
    public val UPLOAD_END_POINT: String     = "${HOST_END_POINT}/tinguploads/"

    //SIGN UP & AUTH ROUTES
    val checkEmailUsername: String          = "${END_POINT}usr/check/email-username/"
    val submitEmailSignUp: String           = "${END_POINT}usr/signup/email/"
    val submitGoogleSignUp: String          = "${END_POINT}usr/signup/google/"
    val authLoginUser: String               = "${END_POINT}usr/auth/login/"
    val authResetPassword                   = "${END_POINT}usr/auth/password/reset/"
    val userMapPin: String                  = "${END_POINT}usr/profile/map/pin/"

    //UPDATE & USER DATA
    val updateProfileImage: String          = "${END_POINT}usr/profile/image/update/"
    val updateProfileEmail: String          = "${END_POINT}usr/profile/email/update/"
    val updateProfilePassword: String       = "${END_POINT}usr/profile/password/update/"
    val updateProfileIdentity: String       = "${END_POINT}usr/profile/identity/update/"
    val addUserAddress: String              = "${END_POINT}usr/profile/address/add/"
    val deleteUserAddress: String           = "${END_POINT}usr/profile/address/delete/"
    val updateUserAddress: String           = "${END_POINT}usr/profile/address/update/"

    //RESTAURANTS
    val restaurantsGlobal: String           = "${END_POINT}usr/g/restaurants/all/"
    val restaurantGet: String               = "${END_POINT}usr/g/restaurants/get/"
    val restaurantMapPin: String            = "${END_POINT}usr/g/restaurants/map/pin/"
    val restaurantFilters: String           = "${END_POINT}usr/g/restaurants/filters/"
    val restaurantsSearchFiltered: String   = "${END_POINT}usr/g/restaurants/search/filter/"

    //CUISINES
    val cuisinesGlobal: String              = "${END_POINT}usr/g/cuisines/all/"
    val cuisineRestaurants: String          = "${END_POINT}usr/g/cuisine/r/"
    val cuisineMenus: String                = "${END_POINT}usr/g/cuisine/m/"

    //DISCOVERY
    val discoverRestaurants: String         = "${END_POINT}usr/d/restaurants/"
    val discoverTopRestaurants: String      = "${END_POINT}usr/d/restaurants/top/"
    val discoverTodayPromosRand: String     = "${END_POINT}usr/d/today/promotions/rand/"
    val discoverTodayPromosAll: String      = "${END_POINT}usr/d/today/promotions/all/"
    val discoverTopMenus: String            = "${END_POINT}usr/d/menus/top/"
    val discoverMenus: String               = "${END_POINT}usr/d/menus/discover/"
}