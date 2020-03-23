package com.codepipes.ting.simpledialogfragment.form;

import android.content.Context;
import android.os.Parcel;
import androidx.annotation.ArrayRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

import com.codepipes.ting.simpledialogfragment.SimpleDialog;
import com.codepipes.ting.simpledialogfragment.color.ColorView;
import com.codepipes.ting.simpledialogfragment.color.SimpleColorDialog;


public class ColorField extends FormElement<ColorField, ColorViewHolder> {

    public static final @ColorInt
    int NONE = ColorView.NONE;
    public static final @ColorInt int AUTO = ColorView.AUTO;

    private @ColorInt int preset = NONE;
    private int presetId = NO_ID;
    protected int[] colors = SimpleColorDialog.DEFAULT_COLORS;
    protected boolean allowCustom = true;
    protected int outline = NONE;

    private ColorField(String resultKey) {
        super(resultKey);
    }




    /**
     * Factory method for a color field.
     *
     * @param key the key that can be used to receive the final state from the bundle in
     *            {@link SimpleDialog.OnDialogResultListener#onResult}
     */
    public static ColorField picker(String key){
        return new ColorField(key);
    }


    /**
     * Sets the initial color
     *
     * @param preset initial state
     */
    public ColorField color(@ColorInt int preset){
        this.preset = preset;
        return this;
    }

    /**
     * Sets the initial color
     *
     * @param presetResourceId initial color as resource
     */
    public ColorField colorRes(@ColorRes int presetResourceId){
        this.presetId = presetResourceId;
        return this;
    }

    /**
     * Sets the colors to choose from
     * Default is the {@link SimpleColorDialog#DEFAULT_COLORS} set
     *
     * @param colors array of rgb-colors
     */
    public ColorField colors(@ColorInt int[] colors){
        this.colors = colors;
        return this;
    }

    /**
     * Sets the color pallet to choose from
     * May be one of {@link SimpleColorDialog#MATERIAL_COLOR_PALLET},
     * {@link SimpleColorDialog#MATERIAL_COLOR_PALLET_DARK},
     * {@link SimpleColorDialog#MATERIAL_COLOR_PALLET_LIGHT},
     * {@link SimpleColorDialog#BEIGE_COLOR_PALLET} or a custom pallet
     *
     * @param context a context to resolve the resource
     * @param colorArrayRes color array resource id
     */
    public ColorField colors(Context context, @ArrayRes int colorArrayRes){
        return colors(context.getResources().getIntArray(colorArrayRes));
    }

    /**
     * Set this to true to show a field with a color picker option
     *
     * @param allow allow custom picked color if true
     */
    public ColorField allowCustom(boolean allow){
        allowCustom = allow;
        return this;
    }

    /**
     * Add a colored outline to the color fields.
     * {@link ColorField#NONE} disables the outline (default)
     * {@link ColorField#AUTO} uses a black or white outline depending on the brightness
     *
     * @param color color int or {@link ColorField#NONE} or {@link ColorField#AUTO}
     */
    public ColorField showOutline(@ColorInt int color){
        outline = color;
        return this;
    }








    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ColorViewHolder buildViewHolder() {
        return new ColorViewHolder(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////


    protected @ColorInt int getInitialColor(Context context){
        if (presetId != NO_ID){
            return context.getResources().getColor(presetId);
        }
        return preset;
    }


    protected ColorField(Parcel in) {
        super(in);
        preset = in.readInt();
        presetId = in.readInt();
        colors = in.createIntArray();
        allowCustom = in.readByte() != 0;
        outline = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(preset);
        dest.writeInt(presetId);
        dest.writeIntArray(colors);
        dest.writeByte((byte) (allowCustom ? 1 : 0));
        dest.writeInt(outline);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ColorField> CREATOR = new Creator<ColorField>() {
        @Override
        public ColorField createFromParcel(Parcel in) {
            return new ColorField(in);
        }

        @Override
        public ColorField[] newArray(int size) {
            return new ColorField[size];
        }
    };

}
