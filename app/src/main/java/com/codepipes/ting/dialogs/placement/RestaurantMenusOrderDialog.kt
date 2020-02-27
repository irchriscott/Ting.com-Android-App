package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.R
import com.codepipes.ting.adapters.placement.RestaurantMenusOrderAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_restaurant_menus_order.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import io.reactivex.subjects.PublishSubject
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import java.util.ArrayList
import java.util.concurrent.TimeUnit


class RestaurantMenusOrderDialog : DialogFragment() {

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

        val menuType = arguments!!.getInt(CurrentRestaurant.MENU_TYPE_KEY)
        val branchId = arguments!!.getInt(CurrentRestaurant.RESTO_BRANCH_KEY)

        view.restaurant_menus_type.text = when (menuType) { 1 -> { "Foods" } 2 -> { "Drinks" } 3 -> { "Dishes" } else -> { "Foods" } }
        view.close_restaurant_menus.setOnClickListener {
            dialog.dismiss()
            restaurantMenusOrderCloseListener.onClose()
        }

        val restaurantMenusObserver = object : DisposableObserver<MutableList<RestaurantMenu>>() {

            override fun onComplete() {}

            override fun onNext(menus: MutableList<RestaurantMenu>) {

                view.shimmer_loader.visibility = View.GONE

                if (menus.size > 0) {

                    view.restaurant_menus.visibility = View.VISIBLE
                    view.empty_data.visibility = View.GONE

                    view.restaurant_menus.layoutManager = LinearLayoutManager(activity)
                    view.restaurant_menus.adapter = RestaurantMenusOrderAdapter(menus, fragmentManager!!, context!!)
                } else {

                    view.restaurant_menus.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    when (menuType) {
                        1 -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                            view.empty_data.empty_text.text = "No Food To Show"
                        }
                        2 -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
                            view.empty_data.empty_text.text = "No Drink To Show"
                        }
                        3 -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            view.empty_data.empty_text.text = "No Dish To Show"
                        }
                        else -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                            view.empty_data.empty_text.text = "No Food To Show"
                        }
                    }
                }
            }

            override fun onError(error: Throwable) {

                view.shimmer_loader.visibility = View.GONE
                view.restaurant_menus.visibility = View.GONE
                view.empty_data.visibility = View.VISIBLE

                when (menuType) {
                    1 -> {
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                        view.empty_data.empty_text.text = "No Food To Show"
                    }
                    2 -> {
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
                        view.empty_data.empty_text.text = "No Drink To Show"
                    }
                    3 -> {
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                        view.empty_data.empty_text.text = "No Dish To Show"
                    }
                    else -> {
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                        view.empty_data.empty_text.text = "No Food To Show"
                    }
                }

                TingToast(context!!, error.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }

        val searchMenusWatcher = object : DisposableObserver<TextViewTextChangeEvent>() {
            override fun onComplete() {}

            override fun onNext(event: TextViewTextChangeEvent) {
                publishSubject.onNext(event.text().toString())
            }

            override fun onError(error: Throwable) {}
        }

        disposable.add(publishSubject.debounce(300, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .switchMap(Function<String, Observable<MutableList<RestaurantMenu>>> {
                return@Function TingClient.getInstance(context!!)
                    .getRestaurantMenusPlacement(branchId, menuType, it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

            }).subscribeWith(restaurantMenusObserver))

        disposable.add(RxTextView.textChangeEvents(view.restaurant_menus_filter)
            .skipInitialValue()
            .debounce(300, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(searchMenusWatcher))

        disposable.add(restaurantMenusObserver)

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