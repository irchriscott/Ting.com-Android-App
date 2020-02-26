package com.codepipes.ting.fragments.animations

import android.view.animation.AccelerateInterpolator
import android.app.Activity
import android.animation.Animator
import android.os.Build
import android.view.animation.DecelerateInterpolator
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.view.*
import com.codepipes.ting.R
import com.codepipes.ting.interfaces.OnFragmentTouched
import com.livefront.bridge.Bridge
import java.util.*
import kotlin.math.hypot


class CircularRevealingFragment : Fragment() {

    internal var listener: OnFragmentTouched? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_restaurants_map, container, false)

        rootView.setBackgroundColor(arguments!!.getInt("color"))

        rootView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onLayoutChange(
                v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int,
                oldRight: Int, oldBottom: Int
            ) {
                v.removeOnLayoutChangeListener(this)
                val cx = arguments!!.getInt("cx")
                val cy = arguments!!.getInt("cy")

                val radius = hypot(right.toDouble(), bottom.toDouble()).toInt()

                val reveal = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0f, radius.toFloat())
                reveal.interpolator = DecelerateInterpolator(2f)
                reveal.duration = 1000
                reveal.start()
            }
        })

        // attach a touch listener
        rootView.setOnTouchListener { _, event ->
            if (listener != null) {
                listener!!.onFragmentTouched(this@CircularRevealingFragment, event.x, event.y)
            }
            true
        }
        return rootView
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity is OnFragmentTouched) {
            listener = activity as OnFragmentTouched
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun prepareUnrevealAnimator(cx: Float, cy: Float): Animator {
        val radius = getEnclosingCircleRadius(view!!, cx.toInt(), cy.toInt())
        val anim = ViewAnimationUtils.createCircularReveal(view, cx.toInt(), cy.toInt(), radius.toFloat(), 0f)
        anim.interpolator = AccelerateInterpolator(2f)
        anim.duration = 1000
        return anim
    }

    private fun getEnclosingCircleRadius(v: View, cx: Int, cy: Int): Int {
        val realCenterX = cx + v.left
        val realCenterY = cy + v.top
        val distanceTopLeft = hypot((realCenterX - v.left).toDouble(), (realCenterY - v.top).toDouble()).toInt()
        val distanceTopRight = Math.hypot((v.right - realCenterX).toDouble(),
            (realCenterY - v.top).toDouble()
        ).toInt()
        val distanceBottomLeft = hypot((realCenterX - v.left).toDouble(),
            (v.bottom - realCenterY).toDouble()
        ).toInt()
        val distanceBottomRight = hypot((v.right - realCenterX).toDouble(),
            (v.bottom - realCenterY).toDouble()
        ).toInt()

        val distances = arrayOf(distanceTopLeft, distanceTopRight, distanceBottomLeft, distanceBottomRight)

        return Collections.max(distances.toMutableList())
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

        fun newInstance(centerX: Int, centerY: Int, color: Int): CircularRevealingFragment {
            val args = Bundle()
            args.putInt("cx", centerX)
            args.putInt("cy", centerY)
            args.putInt("color", color)
            val fragment = CircularRevealingFragment()
            fragment.arguments = args
            return fragment

        }
    }
}