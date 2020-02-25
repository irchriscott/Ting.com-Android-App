package com.codepipes.ting.providers


import com.codepipes.ting.models.Order
import com.codepipes.ting.models.Route
import com.codepipes.ting.utils.Routes
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

public interface TingService {

    @GET("api/v1/usr/po/orders/{token}/all/")
    public fun getOrdersMenuPlacement(
        @Path("token") token : String
    ) : Observable<MutableList<Order>>
}