package com.codepipes.ting.providers


import com.codepipes.ting.models.Order
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.Route
import com.codepipes.ting.utils.Routes
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

public interface TingService {

    @GET("api/v1/usr/g/restaurants/get/{branch}/menus/top/")
    public fun getRestaurantTopMenus(
        @Path("branch") branch: Int
    ) : Observable<MutableList<RestaurantMenu>>


    @GET("api/v1/usr/menu/promotion/get/{promo}/menus/promoted/")
    public fun getPromotionPromotedMenus(
        @Path("promo") promo: Int
    ) : Observable<MutableList<RestaurantMenu>>


    @GET("api/v1/usr/po/placement/orders/all/")
    public fun getOrdersMenuPlacement(
        @Query("token") token : String,
        @Header("Authorization") authorization: String
    ) : Observable<MutableList<Order>>
}