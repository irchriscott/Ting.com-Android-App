package com.codepipes.ting.utils


class Routes {

    companion object {

        public const val HOST_END_POINT: String         = "http://172.20.10.9:8000"
        private const val END_POINT: String             = "${HOST_END_POINT}/api/v1/"
        public const val UPLOAD_END_POINT: String       = "${HOST_END_POINT}/tinguploads/"
        public const val API_HOST_PREFIX: String        = "/api/v1/"

        //SIGN UP & AUTH ROUTES
        const val checkEmailUsername: String            = "${END_POINT}usr/check/email-username/"
        const val submitEmailSignUp: String             = "${END_POINT}usr/signup/email/"
        const val submitGoogleSignUp: String            = "${END_POINT}usr/signup/google/"
        const val authLoginUser: String                 = "${END_POINT}usr/auth/login/"
        const val authResetPassword: String             = "${END_POINT}usr/auth/password/reset/"
        const val userMapPin: String                    = "${END_POINT}usr/profile/map/pin/"

        //UPDATE & USER DATA
        const val updateProfileImage: String            = "${END_POINT}usr/profile/image/update/"
        const val updateProfileEmail: String            = "${END_POINT}usr/profile/email/update/"
        const val updateProfilePassword: String         = "${END_POINT}usr/profile/password/update/"
        const val updateProfileIdentity: String         = "${END_POINT}usr/profile/identity/update/"
        const val addUserAddress: String                = "${END_POINT}usr/profile/address/add/"
        const val deleteUserAddress: String             = "${END_POINT}usr/profile/address/delete/"
        const val updateUserAddress: String             = "${END_POINT}usr/profile/address/update/"

        //RESTAURANTS
        const val restaurantsGlobal: String             = "${END_POINT}usr/g/restaurants/all/"
        const val restaurantGet: String                 = "${END_POINT}usr/g/restaurants/get/"
        const val restaurantMapPin: String              = "${END_POINT}usr/g/restaurants/map/pin/"
        const val restaurantFilters: String             = "${END_POINT}usr/g/restaurants/filters/"
        const val restaurantsSearchFiltered: String     = "${END_POINT}usr/g/restaurants/search/filter/"
        const val checkUserRestaurantReview: String     = "${END_POINT}usr/g/restaurants/reviews/check/"
        const val restaurantTableLocation: String       = "${END_POINT}usr/g/restaurants/tables/locations/"
        const val restaurantBook: String                = "${END_POINT}usr/g/restaurants/book/"

        //CUISINES
        const val cuisinesGlobal: String                = "${END_POINT}usr/g/cuisines/all/"
        const val cuisineRestaurants: String            = "${END_POINT}usr/g/cuisine/r/"
        const val cuisineMenus: String                  = "${END_POINT}usr/g/cuisine/m/"

        //DISCOVERY
        const val discoverRestaurants: String           = "${END_POINT}usr/d/restaurants/"
        const val discoverTopRestaurants: String        = "${END_POINT}usr/d/restaurants/top/"
        const val discoverTodayPromosRand: String       = "${END_POINT}usr/d/today/promotions/rand/"
        const val discoverTodayPromosAll: String        = "${END_POINT}usr/d/today/promotions/all/"
        const val discoverTopMenus: String              = "${END_POINT}usr/d/menus/top/"
        const val discoverMenus: String                 = "${END_POINT}usr/d/menus/discover/"

        //MENU
        const val checkUserMenuReview: String           = "${END_POINT}usr/menu/reviews/check/"

        //PLACEMENT & ORDERS
        const val requestRestaurantTable: String        = "${END_POINT}usr/po/table/request/"
        const val getCurrentPlacement: String           = "${END_POINT}usr/po/placement/get/"
        const val updatePlacementPeople: String         = "${END_POINT}usr/po/placement/people/update/"
        const val restaurantMenusOrders: String         = "${END_POINT}usr/po/orders/branch/menus/"
        const val placeOrderMenu: String                = "${END_POINT}usr/po/orders/menu/place/"
        const val placementOrdersMenu: String           = "${END_POINT}usr/po/placement/orders/all/"
        const val placementGetBill: String              = "${END_POINT}usr/po/placement/bill/"
        const val placementUpdateBillTips: String       = "${END_POINT}usr/po/placement/bill/tips/update/"
        const val placementBillComplete: String         = "${END_POINT}usr/po/placement/bill/complete/"
        const val placementBillRequest: String          = "${END_POINT}usr/po/placement/bill/request/"
        const val placementRequestWaiter: String        = "${END_POINT}usr/po/placement/request/send/"
        const val placementTerminate: String            = "${END_POINT}usr/po/placement/terminate/"

        //MOMENT
        const val momentsSave: String                   = "${END_POINT}usr/m/moments/save/"

        //SEARCH
        const val liveSearchResults: String             = "${END_POINT}usr/g/search/live/"
        const val menusSearchResults: String            = "${END_POINT}usr/g/search/menus/"
        const val restaurantSearchResults: String       = "${END_POINT}usr/g/search/restaurants/"
    }
}