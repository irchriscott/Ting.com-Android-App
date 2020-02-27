package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.adapters.placement.PlacementOrdersMenuAdapter
import com.codepipes.ting.adapters.placement.RestaurantMenusOrderAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.interfaces.PlacementOrdersMenuEventsListener
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.models.Order
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.functions.Function
import kotlinx.android.synthetic.main.fragment_restaurant_menus_order.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import java.util.concurrent.TimeUnit


class PlacementOrdersDialog : DialogFragment() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var restaurantMenusOrderCloseListener: RestaurantMenusOrderCloseListener

    private val disposable = CompositeDisposable()
    private val publishSubject = PublishSubject.create<String>()

    override fun getTheme(): Int = R.style.TransparentDialog

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.fragment_restaurant_menus_order, null, false)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        val placement = UserPlacement(context!!).getToken()

        view.restaurant_menus_type.text = "Orders"
        view.close_restaurant_menus.setOnClickListener {
            dialog.dismiss()
            restaurantMenusOrderCloseListener.onClose()
        }

        val placementOrdersObserver = object : DisposableObserver<MutableList<Order>>() {

            override fun onComplete() {}

            override fun onNext(orders: MutableList<Order>) {

                view.shimmer_loader.visibility = View.GONE

                if(!orders.isNullOrEmpty()) {

                    view.restaurant_menus.visibility = View.VISIBLE
                    view.empty_data.visibility = View.GONE

                    view.restaurant_menus.layoutManager = LinearLayoutManager(activity)
                    view.restaurant_menus.adapter = PlacementOrdersMenuAdapter(orders, fragmentManager!!, object : PlacementOrdersMenuEventsListener{
                        override fun onReorder(order: Int, quantity: Int, conditions: String) {
                            Log.i("REORDER", "${order.toString()} - ${quantity.toString()} - $conditions")
                        }

                        override fun onNotify(order: Int) {
                            Log.i("NOTIFY", order.toString())
                        }

                        override fun onCancel(order: Int) {
                            Log.i("CANCEL", order.toString())
                        }
                    })
                } else {
                    view.restaurant_menus.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                    view.empty_data.empty_image.alpha = 0.6f
                    view.empty_data.empty_text.text = "No Order To Show"
                }
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
                    .getOrdersMenuPlacement(placement!!, it, session.token!!)
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