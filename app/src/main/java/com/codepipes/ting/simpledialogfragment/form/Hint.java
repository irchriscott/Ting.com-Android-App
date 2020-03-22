package com.codepipes.ting.simpledialogfragment.form;

import android.os.Parcel;
import android.support.annotation.StringRes;


@SuppressWarnings({"unused", "WeakerAccess"})
public class Hint extends FormElement<Hint, HintViewHolder> {

    public Hint() {
        super((String) null);
    }

    /**
     * Factory method for a hint.
     *
     */
    public static Hint plain(String hint){
        return new Hint().label(hint);
    }
    public static Hint plain(@StringRes int hint){
        return new Hint().label(hint);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public HintViewHolder buildViewHolder() {
        return new HintViewHolder(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////


    protected Hint(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Hint> CREATOR = new Creator<Hint>() {
        @Override
        public Hint createFromParcel(Parcel in) {
            return new Hint(in);
        }

        @Override
        public Hint[] newArray(int size) {
            return new Hint[size];
        }
    };
}
