package com.codepipes.ting.imageeditor.picchooser;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;


public class SquareImageView extends android.support.v7.widget.AppCompatImageView {

    public SquareImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}
