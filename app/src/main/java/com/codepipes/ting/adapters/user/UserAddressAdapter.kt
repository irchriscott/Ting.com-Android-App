package com.codepipes.ting.adapters.user

import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.interfaces.ActionSheetCallBack
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.ErrorMessage
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.dialogs.user.edit.EditUserAddress
import com.codepipes.ting.models.Address
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.models.UserAddresses
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import kotlinx.android.synthetic.main.row_user_address.view.*
import okhttp3.*
import java.io.IOException

class UserAddressAdapter(private val addresses: UserAddresses) : RecyclerView.Adapter<UserAddressViewHolder>() {

    private val viewBinderHelper = ViewBinderHelper()
    private val addressesList = addresses.addresses as MutableList

    private lateinit var user: User
    private lateinit var userAuthentication: UserAuthentication

    private val mProgressOverlay = ProgressOverlay()

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): UserAddressViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_user_address, parent, false)
        return UserAddressViewHolder(row)
    }

    override fun getItemCount(): Int = addressesList.size

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: UserAddressViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val address = addressesList[position]
        val activity = holder.view.context as Activity

        userAuthentication = UserAuthentication(holder.view.context)
        user = userAuthentication.get()!!

        holder.view.address_name.text = address.address
        holder.address = address
        viewBinderHelper.bind(holder.view.user_address_swipe_reveal, address.id.toString())

        when {
            address.type.toLowerCase() == "home" -> holder.view.address_icon.setImageResource(R.drawable.ic_address_home)
            address.type.toLowerCase() == "school" -> holder.view.address_icon.setImageResource(R.drawable.ic_address_school)
            address.type.toLowerCase() == "work" -> holder.view.address_icon.setImageResource(R.drawable.ic_address_work)
            else -> holder.view.address_icon.setImageResource(R.drawable.ic_address_other)
        }

        val menuList = mutableListOf<String>()
        menuList.add("Delete Address")

        holder.view.address_delete.setOnClickListener {
            ActionSheet(holder.view.context, menuList)
                .setTitle("You won't be able to get notification of this address")
                .setColorData(activity.resources.getColor(R.color.colorGoogleRedTwo))
                .setColorTitleCancel(activity.resources.getColor(R.color.colorGoogleRedTwo))
                .setCancelTitle("Cancel")
                .create(object : ActionSheetCallBack {
                    override fun data(data: String, position: Int) {
                        when(position){
                            0 -> {
                                val url = "${Routes.deleteUserAddress}${address.id}/"
                                val client = OkHttpClient()

                                val request = Request.Builder()
                                    .header("Authorization", user.token!!)
                                    .url(url)
                                    .get()
                                    .build()

                                mProgressOverlay.show(activity.fragmentManager, mProgressOverlay.tag)

                                client.newCall(request).enqueue(object : Callback {

                                    override fun onFailure(call: Call, e: IOException) {
                                        activity.runOnUiThread {
                                            TingToast(
                                                activity,
                                                e.message!!,
                                                TingToastType.ERROR
                                            ).showToast(Toast.LENGTH_LONG)
                                        }
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        val responseBody = response.body()!!.string()
                                        val gson = Gson()
                                        try{
                                            val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                                            activity.runOnUiThread {
                                                mProgressOverlay.dismiss()
                                                if (serverResponse.status == 200){
                                                    addressesList.removeAt(position)
                                                    notifyItemRemoved(position)
                                                    notifyItemRangeRemoved(position, addressesList.size)
                                                    userAuthentication.set(gson.toJson(serverResponse.user))
                                                    TingToast(
                                                        activity,
                                                        serverResponse.message,
                                                        TingToastType.SUCCESS
                                                    ).showToast(
                                                        Toast.LENGTH_LONG)
                                                } else { ErrorMessage(
                                                    activity,
                                                    serverResponse.message
                                                ).show() }
                                            }
                                        } catch (e: Exception){
                                            activity.runOnUiThread {
                                                mProgressOverlay.dismiss()
                                                TingToast(
                                                    activity,
                                                    "An Error Has Occurred",
                                                    TingToastType.ERROR
                                                ).showToast(
                                                    Toast.LENGTH_LONG)
                                            }
                                        }

                                    }
                                })
                            }
                        }

                    }
                })
        }

        holder.view.address_edit.setOnClickListener {
            val mEditUserAddressDialog = EditUserAddress()
            val args = Bundle()
            val appCompatActivity = activity as AppCompatActivity
            args.putString("address", Gson().toJson(address))
            mEditUserAddressDialog.arguments = args
            mEditUserAddressDialog.show(activity.supportFragmentManager, mEditUserAddressDialog.tag)
        }
    }
}


class UserAddressViewHolder(val view: View, var address: Address? = null) : RecyclerView.ViewHolder(view){}