package com.codepipes.ting.dialogs.restaurant

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.activities.restaurant.RestaurantProfile
import com.codepipes.ting.dialogs.messages.SelectDialog
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.interfaces.SelectItemListener
import com.codepipes.ting.interfaces.SubmitReservationListener
import com.codepipes.ting.models.BranchTableLocations
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_book_reservation.view.*
import okhttp3.Interceptor
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class BookReservationDialog : DialogFragment() {

    private var selectedTable: Int? = null
    private lateinit var localData: LocalData
    private lateinit var submitReservationListener: SubmitReservationListener

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_reservation, null, false)

        localData = LocalData(context!!)
        val branchId = arguments?.getInt(RestaurantProfile.BRANCH_ID_KEY)
        val localBranch  = localData.getRestaurant(branchId?:0)

        if(localBranch != null) {
            view.dialog_title.text = "Make Reservation at ${localBranch.restaurant?.name}, ${localBranch.name}"
            view.form_date.hint = "Reservation Date (${localBranch.restaurant?.config?.daysBeforeReservation} days before)"
            view.form_time.hint = "Reservation Time (between ${localBranch.restaurant?.opening} and ${localBranch.restaurant?.closing})"
        }

        val calendar = Calendar.getInstance()

        val date = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val format = "yyyy-MM-dd"
            val sdf = SimpleDateFormat(format, Locale.US)
            view.form_date.setText(sdf.format(calendar.time))
        }

        view.form_date.setOnClickListener {
            DatePickerDialog(activity!!,
                R.style.DatePickerAppTheme, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val time = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            view.form_time.setText("$hourOfDay:$minute")
        }

        view.form_time.setOnClickListener {
            TimePickerDialog(activity!!,
                R.style.DatePickerAppTheme, time, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        val interceptor = Interceptor {
            val url = it.request().url().newBuilder()
                .addQueryParameter("branch", (branchId?:0).toString())
                .build()
            val request = it.request().newBuilder()
                .url(url)
                .build()
            it.proceed(request)
        }

        TingClient.getRequest(Routes.restaurantTableLocation, interceptor, null) { _, isSuccess, result ->
            activity?.runOnUiThread {
                if(isSuccess) {
                    try {
                        val branchTables = Gson().fromJson(result, BranchTableLocations::class.java)
                        val tables = branchTables.locations.filter { branchTables.tables.contains(it.id) }
                        view.form_table_location.setOnClickListener {
                            if(tables.isNotEmpty()){
                                val tablesArray = tables.map { it.name }
                                val selectDialog = SelectDialog()
                                val bundle = Bundle()
                                bundle.putString(CurrentRestaurant.CONFIRM_TITLE_KEY, "Select Table Location")
                                selectDialog.arguments = bundle
                                selectDialog.setItems(tablesArray, object : SelectItemListener {
                                    override fun onSelectItem(position: Int) {
                                        view.form_selected_table.text = tablesArray[position]
                                        selectedTable = tables[position].id
                                        selectDialog.dismiss()
                                    }
                                })
                                selectDialog.show(fragmentManager, selectDialog.tag)
                            } else { TingToast(activity!!, "No Table Found", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                        }
                    } catch (e: Exception) { TingToast(activity!!, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                } else { TingToast(activity!!, result, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
            }
        }

        view.form_close.setOnClickListener { dialog.dismiss() }
        view.form_submit.setOnClickListener {
            if(selectedTable != null) {
                submitReservationListener.onSubmitReservation(
                    view.form_number_of_people.text.toString().replace("\\s", ""),
                    view.form_date.text.toString().replace("\\s", ""),
                    view.form_time.text.toString().replace("\\s", ""),
                    selectedTable!!
                )
            } else { TingToast(context!!, "Please, select table location", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
        }

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

    public fun submitReservation(listener: SubmitReservationListener) {
        submitReservationListener = listener
    }
}