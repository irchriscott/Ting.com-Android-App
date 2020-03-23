package com.codepipes.ting.simpledialogfragment.list;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.ArrayRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class SimpleListDialog extends CustomListDialog<SimpleListDialog> {

    public static final String TAG = "SimpleListDialog.";

    public static final String
            SELECTED_LABELS = TAG + "selectedLabels",
            SELECTED_SINGLE_LABEL = TAG + "selectedSingleLabel";


    public static SimpleListDialog build(){
        return new SimpleListDialog();
    }


    /**
     * Populate the list with the labels provided
     *
     * @param context a context for resolving the string ids (cannot use getContext() here)
     * @param labelsResourceIds a list of android string resource identifiers
     */
    public SimpleListDialog items(Context context, @StringRes int[] labelsResourceIds){
        ArrayList<SimpleListItem> list = new ArrayList<>(labelsResourceIds.length);
        for (int id : labelsResourceIds) {
            list.add(new SimpleListItem(context.getString(id), id));
        }
        return items(list);
    }

    /**
     * Populate the list using a string array resource id
     *
     * @param context a context for resolving the resource id (cannot use getContext() here)
     * @param labelArrayResourceIds an android string array resource identifier
     */
    public SimpleListDialog items(Context context, @ArrayRes int labelArrayResourceIds){
        return items(context.getResources().getStringArray(labelArrayResourceIds));
    }

    /**
     * Populate the list with the labels provided
     *
     * @param labels a list of string to be displayed
     */
    public SimpleListDialog items(String[] labels){
        ArrayList<SimpleListItem> list = new ArrayList<>(labels.length);
        for (String label : labels) {
            list.add(new SimpleListItem(label, label.hashCode()));
        }
        return items(list);
    }

    /**
     * Populate the list with the labels provided
     * The corresponding ids can be used to identify which labels were selected
     *
     * @param labels a list of string to be displayed
     * @param ids a list of ids corresponding to the strings
     *
     * @throws IllegalArgumentException if the arrays length don't match
     */
    public SimpleListDialog items(String[] labels, long[] ids){
        if (labels.length != ids.length){
            throw new IllegalArgumentException("Length of ID-array must match label array length!");
        }
        ArrayList<SimpleListItem> list = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length && i < ids.length; i++) {
            list.add(new SimpleListItem(labels[i], ids[i]));
        }
        return items(list);
    }

    /**
     * Populate the list with the Items provided.
     * See {@link SimpleListItem} for further details
     *
     * @param items a list of {@link SimpleListItem}
     */
    public SimpleListDialog items(ArrayList<SimpleListItem> items){
        getArguments().putParcelableArrayList(DATA_SET, items);
        return this;
    }

    /**
     * If set to true, show an input field at the to of the list and allow the user
     * to filter the list
     *
     * @param enabled weather to allow filtering or not
     * @param highlight weather to highlight the text filtered
     */
    public SimpleListDialog filterable(boolean enabled, boolean highlight) {
        setArg(HIGHLIGHT, highlight);
        return super.filterable(enabled);
    }




    protected final static String
            DATA_SET = TAG + "data_set",
            HIGHLIGHT = TAG + "highlight";

    ArrayList<SimpleListItem> mData;

    @Override
    protected SimpleListAdapter onCreateAdapter() {

        int layout;
        switch (getArguments().getInt(CHOICE_MODE)) {
            case SINGLE_CHOICE:
                layout = android.R.layout.simple_list_item_single_choice;
                break;
            case MULTI_CHOICE:
                layout = android.R.layout.simple_list_item_multiple_choice;
                break;
            case NO_CHOICE:
            case SINGLE_CHOICE_DIRECT:
            default:
                layout = android.R.layout.simple_list_item_1;
                break;
        }


        mData = getArguments().getParcelableArrayList(DATA_SET);
        if (mData == null) mData = new ArrayList<>(0);

        return new SimpleListAdapter(layout, mData);

    }


    @Override
    protected Bundle onResult(int which) {
        Bundle result = super.onResult(which);
        if (result != null) {

            ArrayList<Integer> positions = result.getIntegerArrayList(SELECTED_POSITIONS);
            if (positions != null) {
                ArrayList<String> labels = new ArrayList<>(positions.size());
                for (Integer pos : positions) {
                    labels.add(mData.get(pos).getString());
                }
                result.putStringArrayList(SELECTED_LABELS, labels);
            }

            if (result.containsKey(SELECTED_SINGLE_POSITION)) {
                result.putString(SELECTED_SINGLE_LABEL, mData.get(
                        result.getInt(SELECTED_SINGLE_POSITION)).getString());
            }

        }
        return result;
    }



    class SimpleListAdapter extends AdvancedAdapter<String> {

        private int mLayout;

        SimpleListAdapter(@LayoutRes int layout, ArrayList<SimpleListItem> data){
            mLayout = layout;
            ArrayList<Pair<String, Long>> dataAndIds = new ArrayList<>(data.size());
            for (SimpleListItem simpleListItem : data) {
                dataAndIds.add(new Pair<>(simpleListItem.getString(), simpleListItem.getId()));
            }
            setDataAndIds(dataAndIds);
        }

        AdvancedFilter mFilter = new AdvancedFilter(true, true){

            @Override
            protected boolean matches(String object, @NonNull CharSequence constraint) {
                return matches(object);
            }
        };

        @Override
        public AdvancedFilter getFilter() {
            return mFilter;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView textView;

            if (convertView == null){
                convertView = inflate(mLayout, parent, false);
                textView = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(textView);
            } else {
                textView = (TextView) convertView.getTag();
            }

            if (getArguments().getBoolean(HIGHLIGHT)) {
                textView.setText(highlight(getItem(position), getContext()));
            } else {
                textView.setText(getItem(position));
            }

            return super.getView(position, convertView, parent);
        }

    }
}
