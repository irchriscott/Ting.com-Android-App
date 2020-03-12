package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.adapters.placement.PlacementOrdersMenuAdapter
import com.codepipes.ting.adapters.placement.RestaurantMenusOrderAdapter
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.interfaces.PlacementOrdersMenuEventsListener
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.models.*
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.functions.Function
import kotlinx.android.synthetic.main.fragment_restaurant_menus_order.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit


class PlacementOrdersDialog : DialogFragment() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var restaurantMenusOrderCloseListener: RestaurantMenusOrderCloseListener

    private val disposable = CompositeDisposable()
    private val publishSubject = PublishSubject.create<String>()

    override fun getTheme(): Int = R.style.TransparentDialog

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.fragment_restaurant_menus_order, null, false)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        val placement = UserPlacement(context!!).get()!!

        view.restaurant_menus_type.text = "Orders"
        view.close_restaurant_menus.setOnClickListener {
            dialog.dismiss()
            restaurantMenusOrderCloseListener.onClose()
        }

        val placementOrdersObserver = object : DisposableObserver<MutableList<Order>>() {

            override fun onComplete() {}

            override fun onNext(orders: MutableList<Order>) {
                loadOrders(view, placement, view.restaurant_menus_filter.text.toString(), orders)
            }

            override fun onError(error: Throwable) {

                view.shimmer_loader.visibility = View.GONE

                view.restaurant_menus.visibility = View.GONE
                view.empty_data.visibility = View.VISIBLE

                view.empty_data.empty_image.setImageResource(R.drawable.ic_warning_exclamation_gray)
                view.empty_data.empty_image.alpha = 0.6f
                view.empty_data.empty_text.text = "An Error Occurred"

                TingToast(context!!, error.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }

        val searchOrdersWatcher = object : DisposableObserver<TextViewTextChangeEvent>() {
            override fun onComplete() {}
            override fun onNext(event: TextViewTextChangeEvent) {
                publishSubject.onNext(event.text().toString())
            }
            override fun onError(error: Throwable) {}
        }

        disposable.add(publishSubject.debounce(300, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .switchMap(Function<String, Observable<MutableList<Order>>> {
                return@Function TingClient.getInstance(context!!)
                    .getOrdersMenuPlacement(placement.token, it, session.token!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

            }).subscribeWith(placementOrdersObserver))

        disposable.add(
            RxTextView.textChangeEvents(view.restaurant_menus_filter)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(searchOrdersWatcher))

        disposable.add(placementOrdersObserver)

        publishSubject.onNext("")

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun loadOrders(view: View, placement: Placement, query: String, orders: MutableList<Order>) {

        view.shimmer_loader.visibility = View.GONE

        if(!orders.isNullOrEmpty()) {

            view.restaurant_menus.visibility = View.VISIBLE
            view.empty_data.visibility = View.GONE

            val linearLayoutManager = LinearLayoutManager(activity)
            val placementOrdersMenuAdapter = PlacementOrdersMenuAdapter(orders, fragmentManager!!, object : PlacementOrdersMenuEventsListener{
                override fun onReorder(order: Int, quantity: Int, conditions: String) {
                    TingClient.getInstance(context!!)
                        .rePlaceOrderPlacement(order, quantity, conditions)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : DisposableObserver<ServerResponse>() {
                            override fun onComplete() {
                                publishSubject.onNext(view.restaurant_menus_filter.text.toString())
                            }

                            override fun onNext(response: ServerResponse) {
                                publishSubject.onNext(view.restaurant_menus_filter.text.toString())
                                getOrders(view, placement, query)
                                TingToast(context!!, response.message,
                                    when (response.type) {
                                        "error" -> TingToastType.ERROR
                                        "success" -> TingToastType.SUCCESS
                                        else -> TingToastType.DEFAULT
                                    }
                                ).showToast(Toast.LENGTH_LONG)
                            }

                            override fun onError(error: Throwable) {
                                TingToast(context!!, error.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                            }
                        })
                }

                override fun onNotify(order: Int) {
                    val pubnubConfiguration = PNConfiguration()
                    pubnubConfiguration.publishKey = UtilData.PUBNUB_PUBLISH_KEY
                    pubnubConfiguration.subscribeKey = UtilData.PUBNUB_SUBSCRIBE_KEY
                    pubnubConfiguration.isSecure = true

                    val pubnub = PubNub(pubnubConfiguration)
                    val args = mapOf<String, String?>("table" to placement.table.id.toString(), "token" to placement.token)
                    val data = mapOf<String, String>("table" to placement.table.number)

                    val receiverBranch = SocketUser(placement.table.branch?.id, 1, "${placement.table.branch?.restaurant?.name}, ${placement.table.branch?.name}", placement.table.branch?.email, placement.table.branch?.restaurant?.logo, placement.table.branch?.channel)
                    val messageBranch = SocketResponseMessage(pubnubConfiguration.uuid, UtilData.SOCKET_REQUEST_NOTIFY_ORDER, userAuthentication.socketUser(), receiverBranch, 200, null, args, data)

                    pubnub.publish().channel(placement.table.branch?.channel).message(Gson().toJson(messageBranch))
                        .async(object : PNCallback<PNPublishResult>() {
                            override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                                if (status.isError || status.statusCode != 200) {
                                    TingToast(context!!, "Connection Error Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                }
                            }
                        })

                    val receiverWaiter = SocketUser(placement.waiter?.id, 1, "${placement.waiter?.name}", placement.waiter?.email, placement.waiter?.image, placement.waiter?.channel)
                    val messageWaiter = SocketResponseMessage(pubnubConfiguration.uuid, UtilData.SOCKET_REQUEST_W_NOTIFY_ORDER, userAuthentication.socketUser(), receiverWaiter, 200, null, args, data)

                    pubnub.publish().channel(placement.waiter?.channel).message(Gson().toJson(messageWaiter))
                        .async(object : PNCallback<PNPublishResult>() {
                            override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                                if (status.isError || status.statusCode != 200) {
                                    TingToast(context!!, "Connection Error Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                } else { TingToast(context!!, "Order Notified", TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG) }
                            }
                        })
                }

                override fun onCancel(order: Int, position: Int) {
                    val actionSheet = ActionSheet(context!!, mutableListOf("Cancel This Order"))
                        .setTitle("Cancel Order")
                        .setColorData(activity?.resources!!.getColor(R.color.colorGray))
                        .setColorTitleCancel(activity?.resources!!.getColor(R.color.colorGoogleRedTwo))
                        .setColorSelected(activity?.resources!!.getColor(R.color.colorPrimary))
                        .setCancelTitle("Cancel")

                    actionSheet.create(object : ActionSheetCallBack {
                        override fun data(data: String, position: Int) {
                            activity?.runOnUiThread {
                                TingClient.getInstance(context!!)
                                    .cancelOrderPlacement(order)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : DisposableObserver<ServerResponse>() {
                                        override fun onComplete() {
                                            publishSubject.onNext(view.restaurant_menus_filter.text.toString())
                                        }

                                        override fun onNext(response: ServerResponse) {
                                            publishSubject.onNext(view.restaurant_menus_filter.text.toString())
                                            getOrders(view, placement, query)
                                            TingToast(context!!, response.message,
                                                when (response.type) {
                                                    "error" -> TingToastType.ERROR
                                                    "success" -> TingToastType.SUCCESS
                                                    else -> TingToastType.DEFAULT
                                                }
                                            ).showToast(Toast.LENGTH_LONG)
                                        }

                                        override fun onError(error: Throwable) {
                                            TingToast(context!!, error.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                        }
                                    })
                            }
                        }
                    })
                }
            })

            view.restaurant_menus.layoutManager = linearLayoutManager
            view.restaurant_menus.adapter = placementOrdersMenuAdapter
            ViewCompat.setNestedScrollingEnabled(view.restaurant_menus, false)

            var pageNum = 1

            view.scroll_view.setOnScrollChangeListener { v: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->

                if (v?.getChildAt(v.childCount - 1) != null) {
                    if ((scrollY >= (v.getChildAt(v.childCount - 1)!!.measuredHeight - v.measuredHeight)) && scrollY > oldScrollY) {

                        val visibleItemCount = linearLayoutManager.childCount
                        val totalItemCount = linearLayoutManager.itemCount
                        val pastVisibleItems = linearLayoutManager.findLastVisibleItemPosition()

                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {

                            pageNum++

                            val interceptor = Interceptor {
                                val url = it.request().url().newBuilder()
                                    .addQueryParameter("token", placement.token)
                                    .addQueryParameter("query", query)
                                    .addQueryParameter("page", pageNum.toString())
                                    .build()
                                val request = it.request().newBuilder()
                                    .header("Authorization", session.token!!)
                                    .url(url)
                                    .build()
                                it.proceed(request)
                            }

                            TingClient.getRequest(Routes.placementOrdersMenu, interceptor, session.token) { _, isSuccess, result ->
                                if(isSuccess) {
                                    activity?.runOnUiThread {
                                        try {
                                            val ordersPage = Gson().fromJson<MutableList<Order>>(result, object : TypeToken<MutableList<Order>>(){}.type)
                                            placementOrdersMenuAdapter.addItems(ordersPage)
                                        } catch (e: Exception){}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            view.restaurant_menus.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE

            view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
            view.empty_data.empty_image.alpha = 0.6f
            view.empty_data.empty_text.text = "No Order To Show"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getOrders(view: View, placement: Placement, query: String) {

        val interceptor = Interceptor {
            val url = it.request().url().newBuilder()
                .addQueryParameter("token", placement.token)
                .addQueryParameter("query", query)
                .build()
            val request = it.request().newBuilder()
                .header("Authorization", session.token!!)
                .url(url)
                .build()
            it.proceed(request)
        }

        val url = Routes.placementOrdersMenu
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity?.runOnUiThread {
                    try {
                        val orders = Gson().fromJson<MutableList<Order>>(dataString, object : TypeToken<MutableList<Order>>(){}.type)
                        loadOrders(view, placement, query, orders)
                    } catch (e: Exception) {
                        view.restaurant_menus.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                        view.empty_data.empty_image.alpha = 0.6f
                        view.empty_data.empty_text.text = "No Order To Show"
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onDestroy() {
        disposable.clear()
        super.onDestroy()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        disposable.clear()
        super.onDismiss(dialog)
    }


    fun onDialogClose(listener: RestaurantMenusOrderCloseListener) {
        this.restaurantMenusOrderCloseListener = listener
    }
}