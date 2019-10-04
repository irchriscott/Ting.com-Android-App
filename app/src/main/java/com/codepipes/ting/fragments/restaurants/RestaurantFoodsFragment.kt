package com.codepipes.ting.fragments.restaurants


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.codepipes.ting.R

class RestaurantFoodsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_restaurant_foods, container, false)
    }

    companion object {

        fun newInstance(resto: String) =
            RestaurantFoodsFragment().apply {
                arguments = Bundle().apply {
                    putString("resto", resto)
                }
            }
    }
}
