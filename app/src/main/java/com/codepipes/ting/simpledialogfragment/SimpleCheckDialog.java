package com.codepipes.ting.simpledialogfragment;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.codepipes.ting.R;

public class SimpleCheckDialog extends CustomViewDialog<SimpleCheckDialog> {

    public static final String TAG = "SimpleCheckDialog.";

    public static final String
            CHECKED = TAG + "CHECKED";


    public static SimpleCheckDialog build(){
        return new SimpleCheckDialog();
    }


    /**
     * Sets the initial check state
     *
     * @param preset checkbox initial state
     */
    public SimpleCheckDialog check(boolean preset){ return setArg(CHECKED, preset); }

    /**
     * Sets the checkbox's label
     *
     * @param checkBoxLabel the label as string
     */
    public SimpleCheckDialog label(String checkBoxLabel){ return setArg(CHECKBOX_LABEL, checkBoxLabel); }

    /**
     * Sets the checkbox's label
     *
     * @param checkBoxLabelResourceId the label as android string resource
     */
    public SimpleCheckDialog label(@StringRes int checkBoxLabelResourceId){ return setArg(CHECKBOX_LABEL, checkBoxLabelResourceId); }

    /**
     * Weather the check is required. The positive button will be disabled until the checkbox
     * got checked
     *
     * @param required weather checking the checkbox is required
     */
    public SimpleCheckDialog checkRequired(boolean required){ return setArg(CHECKBOX_REQUIRED, required); }





    protected static final String CHECKBOX_LABEL = "simpleCheckDialog.check_label";
    protected static final String CHECKBOX_REQUIRED = "simpleCheckDialog.check_required";

    private CheckBox mCheckBox;


    private boolean canGoAhead() {
        return mCheckBox.isChecked() || !getArguments().getBoolean(CHECKBOX_REQUIRED);
    }

    @Override
    public View onCreateContentView(Bundle savedInstanceState) {
        // inflate and set your custom view here

        View view = inflate(R.layout.simpledialogfragment_check_box);
        mCheckBox = (CheckBox) view.findViewById(R.id.checkBox);

        mCheckBox.setText(getArgString(CHECKBOX_LABEL));

        if (savedInstanceState != null){
            mCheckBox.setChecked(savedInstanceState.getBoolean(CHECKED, false));
        } else {
            mCheckBox.setChecked(getArguments().getBoolean(CHECKED, false));
        }

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setPositiveButtonEnabled(canGoAhead());
            }
        });

        return view;
    }

    @Override
    protected void onDialogShown() {
        setPositiveButtonEnabled(canGoAhead());
    }

    @Override
    public Bundle onResult(int which) {
        Bundle result = new Bundle();
        result.putBoolean(CHECKED, mCheckBox.isChecked());
        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHECKED, mCheckBox.isChecked());
    }
}
