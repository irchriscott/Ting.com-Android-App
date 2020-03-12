package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.adapters.placement.bill.BillOrdersTableViewAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.events.BillOrdersTableViewListener
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.models.Bill
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_placement_bill.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.Interceptor
import java.lang.Exception
import java.util.concurrent.TimeUnit

class PlacementBillDialog : DialogFragment() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var restaurantMenusOrderCloseListener: RestaurantMenusOrderCloseListener

    override fun getTheme(): Int = R.style.TransparentDialog

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.fragment_placement_bill, null, false)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        val placement = UserPlacement(context!!).get()!!

        val interceptor = Interceptor {
            val url = it.request().url().newBuilder()
                .addQueryParameter("token", placement.token)
                .build()
            val request = it.request().newBuilder()
                .header("Authorization", session.token!!)
                .url(url)
                .build()
            it.proceed(request)
        }

        TingClient.getRequest(Routes.placementGetBill, interceptor, session.token) { _, isSuccess, result ->
            activity?.runOnUiThread {
                if (isSuccess) {
                    try {
                        val bill = Gson().fromJson(result, Bill::class.java)
                        if (bill.id <= 0) { throw Exception("Bill Is Null") }
                        view.placement_bill_progress.visibility = View.GONE
                        view.empty_data.visibility = View.GONE
                        view.placement_bill_content.visibility = View.VISIBLE

                        view.bill_number.text = "Bill No ${bill.number}"

                        val tableViewAdapter = BillOrdersTableViewAdapter(context)
                        view.placement_bill_orders.adapter = tableViewAdapter
                        if (!bill.orders?.orders.isNullOrEmpty()) { tableViewAdapter.setOrdersList(bill.orders?.orders!!.reversed()) }
                        view.placement_bill_orders.tableViewListener = BillOrdersTableViewListener(view.placement_bill_orders)

                    } catch (e: Exception) {
                        view.placement_bill_content.visibility = View.GONE
                        view.placement_bill_progress.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                        view.empty_data.empty_image.alpha = 0.6f
                        view.empty_data.empty_text.text = "No Order Made So Far"

                        try {
                            val serverResponse = Gson().fromJson<ServerResponse>(result, ServerResponse::class.java)
                            TingToast(context!!, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        } catch (e: Exception) { TingToast(context!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                    }
                } else {
                    view.placement_bill_content.visibility = View.GONE
                    view.placement_bill_progress.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                    view.empty_data.empty_image.alpha = 0.6f
                    view.empty_data.empty_text.text = result
                    TingToast(context!!, result, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }
        }

        if (view.bill_number.text == "0001") {

            TingClient.getInstance(context!!)
                .getPlacementBill(placement.token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : DisposableObserver<Bill>() {
                    override fun onComplete() {}

                    override fun onNext(bill: Bill) {
                        view.placement_bill_progress.visibility = View.GONE
                        view.empty_data.visibility = View.GONE
                        view.placement_bill_content.visibility = View.VISIBLE

                        view.bill_number.text = "Bill No ${bill.number}"
                    }

                    override fun onError(e: Throwable) {
                        view.placement_bill_content.visibility = View.GONE
                        view.placement_bill_progress.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                        view.empty_data.empty_image.alpha = 0.6f
                        view.empty_data.empty_text.text = "No Order Made So Far"
                        TingToast(context!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                })
        }

        view.close_placement_bill.setOnClickListener { restaurantMenusOrderCloseListener.onClose() }

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

    fun onDialogClose(listener: RestaurantMenusOrderCloseListener) {
        this.restaurantMenusOrderCloseListener = listener
    }
}