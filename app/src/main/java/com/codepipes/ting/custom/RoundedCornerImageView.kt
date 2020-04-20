package com.codepipes.ting.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import com.codepipes.ting.R


class RoundedCornerImageView : ImageView {

    public var radius = 4.0f
    private var path: Path? = null
    private var rect: RectF? = null

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        val attributeSet = context.obtainStyledAttributes(
            attrs, R.styleable.RoundedCornerImageView, defStyle, 0
        )

        path = Path()
        radius = attributeSet.getFloat(R.styleable.RoundedCornerImageView_imageRadius, 4.0f)

        attributeSet.recycle()
    }

    @SuppressLint("DrawAllocation")
    protected override fun onDraw(canvas: Canvas) {
        rect = RectF(0f, 0f, this.width.toFloat(), this.height.toFloat())
        path!!.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.clipPath(path!!)
        super.onDraw(canvas)
    }
}

