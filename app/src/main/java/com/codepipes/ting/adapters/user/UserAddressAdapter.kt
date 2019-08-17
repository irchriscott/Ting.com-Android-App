package com.codepipes.ting.adapters.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.ErrorMessage
import com.codepipes.ting.dialogs.ProgressOverlay
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.dialogs.user.edit.EditUserAddress
import com.codepipes.ting.models.Address
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.models.UserAddresses
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ting_dot_com.*
import kotlinx.android.synthetic.main.row_user_address.view.*
import okhttp3.*
import java.io.IOException
import java.text.FieldPosition
import java.util.concurrent.TimeUnit

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

        holder.view.address_delete.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(holder.view.context)
            alertDialogBuilder.setTitle("Delete Address")
            alertDialogBuilder.setMessage("Do you really want to delete this address ?")
            alertDialogBuilder.setPositiveButton("YES", DialogInterface.OnClickListener { _, _ ->

                val url = "${Routes().deleteUserAddress}${address.id}/"
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .header("Authorization", user.token)
                    .url(url)
                    .get()
                    .build()

                mProgressOverlay.show(activity.fragmentManager, mProgressOverlay.tag)

                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        activity.runOnUiThread {
                            TingToast(activity, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
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
                                    TingToast(activity, serverResponse.message, TingToastType.SUCCESS).showToast(
                                        Toast.LENGTH_LONG)
                                } else { ErrorMessage(activity, serverResponse.message).show() }
                            }
                        } catch (e: Exception){
                            activity.runOnUiThread {
                                mProgressOverlay.dismiss()
                                TingToast(activity, "An Error Has Occurred", TingToastType.ERROR).showToast(
                                    Toast.LENGTH_LONG)
                            }
                        }

                    }
                })
            })
            alertDialogBuilder.setNegativeButton("NO", DialogInterface.OnClickListener { _, _ ->  })
            val alertDialog: AlertDialog = alertDialogBuilder.create()
            alertDialog.show()
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