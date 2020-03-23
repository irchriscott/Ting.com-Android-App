package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import androidx.fragment.app.DialogFragment
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.adapters.placement.bill.BillExtrasTableViewAdapter
import com.codepipes.ting.adapters.placement.bill.BillOrdersTableViewAdapter
import com.codepipes.ting.dialogs.messages.ConfirmDialog
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.events.BillOrdersTableViewListener
import com.codepipes.ting.interfaces.ConfirmDialogListener
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.interfaces.SubmitPeoplePlacementListener
import com.codepipes.ting.models.Bill
import com.codepipes.ting.models.Placement
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
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

class PlacementBillDialog : DialogFragment() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var placement: Placement
    private lateinit var restaurantMenusOrderCloseListener: RestaurantMenusOrderCloseListener

    private lateinit var interceptor: Interceptor

    override fun getTheme(): Int = R.style.TransparentDialog

    @SuppressLint("SetTextI18n", "InflateParams", "DefaultLocale")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.fragment_placement_bill, null, false)

        userAuthentication = UserAuthentication(activity!!)
        session = userAuthentication.get()!!

        placement = UserPlacement(activity!!).get()!!

        interceptor = Interceptor {
            val url = it.request().url.newBuilder()
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
                        showPlacementBill(bill, view)
                    } catch (e: Exception) {
                        view.placement_bill_content.visibility = View.GONE
                        view.placement_bill_progress.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE
                        view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                        view.empty_data.empty_image.alpha = 0.6f
                        view.empty_data.empty_text.text = "No Order Made So Far"

                        try {
                            val serverResponse = Gson().fromJson<ServerResponse>(result, ServerResponse::class.java)
                            TingToast(activity!!, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        } catch (e: Exception) { TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                    }
                } else {
                    view.placement_bill_content.visibility = View.GONE
                    view.placement_bill_progress.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                    view.empty_data.empty_image.alpha = 0.6f
                    view.empty_data.empty_text.text = result
                    TingToast(activity!!, result, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }
        }

        if (view.bill_number.text == "0001") {

            TingClient.getInstance(activity!!)
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
                        TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                })
        }

        view.close_placement_bill.setOnClickListener { restaurantMenusOrderCloseListener.onClose() }

        return view
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun showPlacementBill(bill: Bill, view: View) {

        view.placement_bill_progress.visibility = View.GONE
        view.empty_data.visibility = View.GONE
        view.placement_bill_content.visibility = View.VISIBLE

        view.bill_number.text = "Bill No ${bill.number}"

        val ordersTableViewAdapter = BillOrdersTableViewAdapter(activity)
        view.placement_bill_orders.adapter = ordersTableViewAdapter
        if (!bill.orders?.orders.isNullOrEmpty()) { ordersTableViewAdapter.setOrdersList(bill.orders?.orders!!.reversed()) }
        view.placement_bill_orders.tableViewListener = BillOrdersTableViewListener(view.placement_bill_orders)

        if(bill.extras.isNotEmpty()) {
            view.placement_bill_extras_title.visibility = View.VISIBLE
            view.placement_bill_extras.visibility = View.VISIBLE

            val extrasTableViewAdapter = BillExtrasTableViewAdapter(activity)
            view.placement_bill_extras.adapter = extrasTableViewAdapter
            extrasTableViewAdapter.setExtrasList(bill.extras, bill.currency)

            view.placement_bill_extras.tableViewListener = BillOrdersTableViewListener(view.placement_bill_extras)
        }

        view.placement_bill_amount.text = "${bill.currency} ${NumberFormat.getNumberInstance().format(bill.amount)}".toUpperCase()
        val discount = (bill.amount * bill.discount) / 100
        view.placement_bill_discount.text = "${bill.currency} ${NumberFormat.getNumberInstance().format(discount)}".toUpperCase()
        view.placement_bill_tips.text = "${bill.currency} ${NumberFormat.getNumberInstance().format(bill.tips)}".toUpperCase()
        view.placement_bill_extras_total.text = "${bill.currency} ${NumberFormat.getNumberInstance().format(bill.extrasTotal)}".toUpperCase()

        val total = bill.amount - bill.discount + bill.tips + bill.extrasTotal
        view.placement_bill_total.text = "${bill.currency} ${NumberFormat.getNumberInstance().format(total)}".toUpperCase()

        view.placement_bill_tips.setOnClickListener {
            val placementPeopleDialog = PlacementPeopleDialog()
            val bundle =  Bundle()
            bundle.putString(CurrentRestaurant.PEOPLE_VALUE_KEY, bill.tips.toString())
            bundle.putString(CurrentRestaurant.PEOPLE_TITLE_KEY, "Enter Tip Amount")
            placementPeopleDialog.arguments = bundle
            placementPeopleDialog.show(fragmentManager!!, placementPeopleDialog.tag)
            placementPeopleDialog.onSubmitPeople(object : SubmitPeoplePlacementListener {
                override fun onSubmit(people: String) {
                    placementPeopleDialog.dismiss()
                    if(people != "" && people.toDouble().toInt() > 1) {
                        val data = hashMapOf<String, String>("token" to placement.token, "tips" to people)
                        TingClient.postRequest(Routes.placementUpdateBillTips, data, null, session.token) { _, isSuccess, result ->
                            activity?.runOnUiThread {
                                if(isSuccess) {
                                    try {
                                        val billData = Gson().fromJson(result, Bill::class.java)
                                        if (billData.id <= 0) { throw Exception("An Error Occurred") }
                                        TingToast(activity!!, "Tip Updated Successfully !!!", TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG)
                                        showPlacementBill(billData, view)
                                    } catch (e: Exception) {
                                        try {
                                            val serverResponse = Gson().fromJson<ServerResponse>(result, ServerResponse::class.java)
                                            TingToast(activity!!, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                        } catch (e: Exception) { TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                                    }
                                } else { TingToast(activity!!, result, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                            }
                        }
                    } else { TingToast(activity!!, "Tip Cannot Be Empty Or 0", TingToastType.WARNING).showToast(Toast.LENGTH_LONG) }
                }
            })
        }

        if (bill.isComplete){
            view.placement_bill_complete.isClickable = false
            view.placement_bill_complete.alpha = 0.6f
        } else {
            view.placement_bill_complete.setOnClickListener {
                val confirmDialog = ConfirmDialog()
                val bundle = Bundle()
                bundle.putString(CurrentRestaurant.CONFIRM_TITLE_KEY, "Complete Bill")
                bundle.putString(CurrentRestaurant.CONFIRM_MESSAGE_KEY, "Dou you really want to complete this bill?\nAfter Completion, No More Order Can Be Made Again.")
                confirmDialog.arguments = bundle
                confirmDialog.show(fragmentManager!!, confirmDialog.tag)
                confirmDialog.onDialogListener(object : ConfirmDialogListener {
                    override fun onAccept() {
                        TingClient.getRequest(Routes.placementBillComplete, interceptor, session.token) { _, isSuccess, result ->
                            activity?.runOnUiThread {
                                confirmDialog.dismiss()
                                if(isSuccess) {
                                    try {
                                        val billData = Gson().fromJson(result, Bill::class.java)
                                        if (billData.id <= 0) { throw Exception("An Error Occurred") }
                                        TingToast(activity!!, "Bill Completed Successfully !!!", TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG)
                                        showPlacementBill(billData, view)
                                    } catch (e: Exception) {
                                        try {
                                            val serverResponse = Gson().fromJson<ServerResponse>(result, ServerResponse::class.java)
                                            TingToast(activity!!, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                        } catch (e: Exception) { TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                                    }
                                } else { TingToast(activity!!, result, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                            }
                        }
                    }

                    override fun onCancel() { confirmDialog.dismiss() }
                })
            }
        }

        view.placement_bill_request.setOnClickListener {
            val confirmDialog = ConfirmDialog()
            val bundle = Bundle()
            bundle.putString(CurrentRestaurant.CONFIRM_TITLE_KEY, "Request Bill")
            bundle.putString(CurrentRestaurant.CONFIRM_MESSAGE_KEY, "Dou you really want to request this bill?\nPlease, Make sure the tip amount has been updated.")
            confirmDialog.arguments = bundle
            confirmDialog.show(fragmentManager!!, confirmDialog.tag)
            confirmDialog.onDialogListener(object : ConfirmDialogListener {
                override fun onAccept() {
                    TingClient.getRequest(Routes.placementBillRequest, interceptor, session.token) { _, isSuccess, result ->
                        activity?.runOnUiThread {
                            confirmDialog.dismiss()
                            if(isSuccess) {
                                try {
                                    val billData = Gson().fromJson(result, Bill::class.java)
                                    if (billData.id <= 0) { throw Exception("An Error Occurred") }
                                    TingToast(activity!!, "Bill Requested Successfully !!!", TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG)
                                    showPlacementBill(billData, view)
                                } catch (e: Exception) {
                                    try {
                                        val serverResponse = Gson().fromJson<ServerResponse>(result, ServerResponse::class.java)
                                        TingToast(activity!!, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                    } catch (e: Exception) { TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                                }
                            } else { TingToast(activity!!, result, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                        }
                    }
                }

                override fun onCancel() { confirmDialog.dismiss() }
            })
        }
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog?.window!!.setLayout(width, height)
        }
    }

    fun onDialogClose(listener: RestaurantMenusOrderCloseListener) {
        this.restaurantMenusOrderCloseListener = listener
    }
}