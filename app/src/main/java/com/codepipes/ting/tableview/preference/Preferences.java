package com.codepipes.ting.tableview.preference;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Preferences implements Parcelable {

    public int rowPosition;
    public int rowPositionOffset;
    public int columnPosition;
    public int columnPositionOffset;
    public int selectedRowPosition;
    public int selectedColumnPosition;

    public Preferences() {
    }

    protected Preferences(Parcel in) {
        rowPosition = in.readInt();
        rowPositionOffset = in.readInt();
        columnPosition = in.readInt();
        columnPositionOffset = in.readInt();
        selectedRowPosition = in.readInt();
        selectedColumnPosition = in.readInt();
    }

    @NonNull
    public static final Creator<Preferences> CREATOR = new Creator<Preferences>() {
        @NonNull
        @Override
        public Preferences createFromParcel(Parcel in) {
            return new Preferences(in);
        }

        @NonNull
        @Override
        public Preferences[] newArray(int size) {
            return new Preferences[size];
        }
    };


    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation. For example, if the object will
     * include a file descriptor in the output of {@link #writeToParcel(Parcel, int)},
     * the return value of this method must include the
     * {@link #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled by this Parcelable
     * object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *              #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(rowPosition);
        dest.writeInt(rowPositionOffset);
        dest.writeInt(columnPosition);
        dest.writeInt(columnPositionOffset);
        dest.writeInt(selectedRowPosition);
        dest.writeInt(selectedColumnPosition);
    }
}
