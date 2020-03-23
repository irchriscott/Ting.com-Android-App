package com.codepipes.ting.customclasses


import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout


class FlingBehavior : AppBarLayout.Behavior {

    private var isPositive = false

    constructor() {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        var velocityYFloat = velocityY
        var consumedBool = consumed
        if (velocityYFloat > 0 && !isPositive || velocityYFloat < 0 && isPositive) {
            velocityYFloat *= -1
        }
        if (target is RecyclerView && velocityYFloat < 0) {
            val recyclerView = target as RecyclerView
            val firstChild: View = recyclerView.getChildAt(0)
            val childAdapterPosition = recyclerView.getChildAdapterPosition(firstChild)
            consumedBool = childAdapterPosition > TOP_CHILD_FLING_THRESHOLD
        }
        return super.onNestedFling(
            coordinatorLayout,
            child, target, velocityX, velocityYFloat, consumedBool
        )
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed)
        isPositive = dy > 0
    }

    companion object {
        private const val TOP_CHILD_FLING_THRESHOLD = 3
    }
}