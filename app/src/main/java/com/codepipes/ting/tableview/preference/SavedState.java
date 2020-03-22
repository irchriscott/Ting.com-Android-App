package com.codepipes.ting.tableview.preference;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.View;


public class SavedState extends View.BaseSavedState {

    public Preferences preferences;

    public SavedState(Parcelable superState) {
        super(superState);
    }

    private SavedState(Parcel in) {
        super(in);
        preferences = in.readParcelable(Preferences.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeParcelable(preferences, flags);
    }

    @NonNull
    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
        @NonNull
        public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
        }

        @NonNull
        public SavedState[] newArray(int size) {
            return new SavedState[size];
        }
    };
}
