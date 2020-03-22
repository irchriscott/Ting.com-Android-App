package com.codepipes.ting.simpledialogfragment.form;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.codepipes.ting.R;


@SuppressWarnings("WeakerAccess")
class HintViewHolder extends FormElementViewHolder<Hint> {

    HintViewHolder(Hint field) {
        super(field);
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.simpledialogfragment_form_item_hint;
    }

    @Override
    protected void setUpView(View view, Context context, Bundle savedInstanceState,
                             final SimpleFormDialog.DialogActions actions) {

        TextView label = (TextView) view.findViewById(R.id.label);
        label.setHint(field.getText(context));

    }


    @Override
    protected void saveState(Bundle outState) {
    }

    @Override
    protected void putResults(Bundle results, String key) {
    }

    @Override
    protected boolean focus(SimpleFormDialog.FocusActions actions) {
        return false;
    }

    @Override
    protected boolean posButtonEnabled(Context context) {
        return true;
    }

    @Override
    protected boolean validate(Context context) {
        return true;
    }

}
