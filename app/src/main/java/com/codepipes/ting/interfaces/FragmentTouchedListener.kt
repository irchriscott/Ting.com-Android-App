package com.codepipes.ting.interfaces

import android.support.v4.app.Fragment

interface OnFragmentTouched {
    fun onFragmentTouched(fragment: Fragment, x: Float, y: Float)
}