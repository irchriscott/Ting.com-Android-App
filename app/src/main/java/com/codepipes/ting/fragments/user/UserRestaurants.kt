package com.codepipes.ting.fragments.user


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.adapters.user.UserRestaurantsAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.User
import com.codepipes.ting.models.UserRestaurant
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_user_restaurants.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit


class UserRestaurants : Fragment() {

    private lateinit var gson: Gson
    private lateinit var user: User

    private lateinit var restaurants: List<UserRestaurant>

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_restaurants, container, false)

        userAuthentication = UserAuthentication(activity!!)
        session = userAuthentication.get()!!

        gson = Gson()
        user = gson.fromJson(arguments?.getString("user"), User::class.java)

        if(user.restaurants != null) {
            val restos = user.restaurants!!
            if(restos.restaurants != null){
                restaurants = restos.restaurants
                showRestaurants(restaurants.toMutableList(), view)
            }
        }

        if(user.id == session.id){
            this.loadUser("${Routes.HOST_END_POINT}${user.urls.apiGetAuth}", view, session.token!!)
        } else { this.loadUser("${Routes.HOST_END_POINT}${user.urls.apiGet}", view, session.token!!) }

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showRestaurants(restos: MutableList<UserRestaurant>?, view: View){
        view.shimmerLoader.stopShimmer()
        view.shimmerLoader.visibility = View.GONE
        if(!restos.isNullOrEmpty()){
            view.user_restaurants_recycler_view.visibility = View.VISIBLE
            view.empty_data.visibility = View.GONE
            view.user_restaurants_recycler_view.layoutManager = LinearLayoutManager(context)
            view.user_restaurants_recycler_view.adapter = UserRestaurantsAdapter(restos.toMutableList())
        } else {
            view.user_restaurants_recycler_view.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
            view.empty_data.empty_text.text = "No Restaurant To Show"
            TingToast(
                context!!,
                "No Restaurant To Show",
                TingToastType.DEFAULT
            ).showToast(Toast.LENGTH_LONG)
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadUser(url: String, view: View, token: String){
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder()
            .header("Authorization", token)
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.shimmerLoader.stopShimmer()
                    view.shimmerLoader.visibility = View.GONE
                    view.user_restaurants_recycler_view.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                    view.empty_data.empty_text.text = "No Restaurant To Show"
                    TingToast(
                        context!!,
                        e.message!!.capitalize(),
                        TingToastType.ERROR
                    ).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                user = gson.fromJson(dataString, User::class.java)
                activity?.runOnUiThread{
                    showRestaurants(user.restaurants?.restaurants?.toMutableList(), view)
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }

    companion object {

        fun newInstance(user: String) =
            UserRestaurants().apply {
                arguments = Bundle().apply {
                    putString("user", user)
                }
            }
    }
}
