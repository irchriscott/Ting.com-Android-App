package com.codepipes.ting.interfaces

import androidx.fragment.app.Fragment

interface FragmentTouchedListener {
    public fun onFragmentTouched(fragment: Fragment, x: Float, y: Float)
}