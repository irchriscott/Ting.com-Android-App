package com.codepipes.ting.simpledialogfragment.form;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.BoolRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.codepipes.ting.simpledialogfragment.SimpleDialog;


public class Check extends FormElement<Check, CheckViewHolder> {

    private String text = null;
    private int textResourceId = NO_ID;
    private Boolean preset = null;
    private int presetId = NO_ID;

    private Check(String resultKey) {
        super(resultKey);
    }

    /**
     * Factory method for a check field.
     *
     * @param key the key that can be used to receive the final state from the bundle in
     *            {@link SimpleDialog.OnDialogResultListener#onResult}
     */
    public static Check box(String key){
        return new Check(key);
    }


    /**
     * Sets the initial state of the checkbox
     *
     * @param preset initial state
     */
    public Check check(boolean preset){
        this.preset = preset;
        return this;
    }

    /**
     * Sets the initial state of the checkbox
     *
     * @param preset initial state as boolean resource
     */
    public Check check(@BoolRes int preset){
        this.presetId = preset;
        return this;
    }

    /**
     * Sets the label
     *
     * @param text label text as string
     */
    public Check label(String text){
        this.text = text;
        return this;
    }

    /**
     * Sets the label
     *
     * @param textResourceId label text as android string resource
     */
    public Check label(@StringRes int textResourceId){
        this.textResourceId = textResourceId;
        return this;
    }













    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CheckViewHolder buildViewHolder() {
        return new CheckViewHolder(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    protected String getText(Context context){
        if (text != null) {
            return text;
        } else if (textResourceId != NO_ID){
            return context.getString(textResourceId);
        }
        return null;
    }

    protected boolean getInitialState(Context context){
        if (preset != null) {
            return preset;
        } else if (presetId != NO_ID){
            return context.getResources().getBoolean(presetId);
        }
        return false;
    }




    private Check(Parcel in) {
        super(in);
        text = in.readString();
        textResourceId = in.readInt();
        byte b = in.readByte();
        preset = b < 0 ? null : b != 0;
        presetId = in.readInt();
    }

    public static final Creator<Check> CREATOR = new Creator<Check>() {
        @Override
        public Check createFromParcel(Parcel in) {
            return new Check(in);
        }

        @Override
        public Check[] newArray(int size) {
            return new Check[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(text);
        dest.writeInt(textResourceId);
        dest.writeByte((byte) (preset == null ? -1 : (preset ? 1 : 0)));
        dest.writeInt(presetId);
    }


}
