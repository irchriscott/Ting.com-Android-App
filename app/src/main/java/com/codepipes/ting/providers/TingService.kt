package com.codepipes.ting.providers


import com.codepipes.ting.models.Order
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.Route
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.utils.Routes
import io.reactivex.Observable
import retrofit2.http.*

public interface TingService {

    @GET("api/v1/usr/g/restaurants/get/{branch}/menus/top/")
    public fun getRestaurantTopMenus(
        @Path("branch") branch: Int
    ) : Observable<MutableList<RestaurantMenu>>


    @GET("api/v1/usr/menu/promotion/get/{promo}/menus/promoted/")
    public fun getPromotionPromotedMenus(
        @Path("promo") promo: Int
    ) : Observable<MutableList<RestaurantMenu>>


    @GET("api/v1/usr/po/orders/branch/menus/")
    public fun getRestaurantMenusPlacement(
        @Query("branch") branch: Int,
        @Query("type") type: Int,
        @Query("query") query: String
    ) : Observable<MutableList<RestaurantMenu>>


    @GET("api/v1/usr/po/placement/orders/all/")
    public fun getOrdersMenuPlacement(
        @Query("token") token : String,
        @Query("query") query: String,
        @Header("Authorization") authorization: String
    ) : Observable<MutableList<Order>>


    @FormUrlEncoded
    @POST("api/v1/usr/po/placement/order/{order}/re/place/")
    public fun rePlaceOrderPlacement(
        @Path("order") order: Int,
        @Field("quantity") quantity: Int,
        @Field("conditions") conditions: String
    ) : Observable<ServerResponse>


    @FormUrlEncoded
    @POST("api/v1/usr/po/placement/order/{order}/cancel/")
    public fun cancelOrderPlacement(
        @Path("order") order: Int,
        @Field("order") orderId: Int
    ) : Observable<ServerResponse>
}