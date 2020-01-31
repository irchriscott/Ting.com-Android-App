package com.codepipes.ting.pageindicatorview.draw.drawer.type;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import com.codepipes.ting.pageindicatorview.animation.data.Value;
import com.codepipes.ting.pageindicatorview.animation.data.type.DropAnimationValue;
import com.codepipes.ting.pageindicatorview.draw.data.Indicator;
import com.codepipes.ting.pageindicatorview.draw.data.Orientation;
import com.codepipes.ting.pageindicatorview.draw.drawer.type.BaseDrawer;

public class DropDrawer extends com.codepipes.ting.pageindicatorview.draw.drawer.type.BaseDrawer {

    public DropDrawer(@NonNull Paint paint, @NonNull Indicator indicator) {
        super(paint, indicator);
    }

    public void draw(
            @NonNull Canvas canvas,
            @NonNull Value value,
            int coordinateX,
            int coordinateY) {

        if (!(value instanceof DropAnimationValue)) {
            return;
        }

        DropAnimationValue v = (DropAnimationValue) value;
        int unselectedColor = indicator.getUnselectedColor();
        int selectedColor = indicator.getSelectedColor();
        float radius = indicator.getRadius();

        paint.setColor(unselectedColor);
        canvas.drawCircle(coordinateX, coordinateY, radius, paint);

        paint.setColor(selectedColor);
        if (indicator.getOrientation() == Orientation.HORIZONTAL) {
            canvas.drawCircle(v.getWidth(), v.getHeight(), v.getRadius(), paint);
        } else {
            canvas.drawCircle(v.getHeight(), v.getWidth(), v.getRadius(), paint);
        }
    }
}
