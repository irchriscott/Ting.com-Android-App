package com.codepipes.ting.simpledialogfragment.input;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.codepipes.ting.R;
import com.codepipes.ting.customclasses.PinEntryEditText;
import com.codepipes.ting.simpledialogfragment.CustomViewDialog;
import com.google.android.material.textfield.TextInputLayout;


public class SimplePinDialog extends CustomViewDialog<SimplePinDialog> {

    public static final String TAG = "SimplePinDialog.";

    public static final String
            PIN = TAG + "pin";

    public static SimplePinDialog build(){
        return new SimplePinDialog();
    }


    /**
     * Sets the pin codes length (default is 4 digits)
     *
     * @param length the code length
     */
    public SimplePinDialog length(int length){ return setArg(LENGTH, length); }

    /**
     * Sets the required pin to check for.
     * When set, the dialog will not close with BUTTON_POSITIVE until this exact pin was entered.
     *
     * @param pin the correct pin
     */
    public SimplePinDialog pin(String pin){
        if (pin != null) {
            length(pin.length());
        }
        return setArg(CHECK_PIN, pin);
    }

//    /**
//     * Sets a mask to show instead of the digits
//     *
//     * @param mask a string (default is *) or android resource id
//     */ // TODO: currently not supported by PinEntryEditText
//    public SimplePinDialog mask(String mask){ return setArg(MASK, mask); }
//    public SimplePinDialog mask(@StringRes int maskResId){ return setArg(MASK, maskResId); }



    protected static final String
            MASK = TAG + "mask",
            LENGTH = TAG + "length",
            CHECK_PIN = TAG + "checkPin";

    private PinEntryEditText mInput;
    private TextInputLayout mInputLayout;

    public SimplePinDialog(){
        title(R.string.pin);
//        mask("*")
        pos(null);
    }

    protected String onValidateInput(@Nullable String input){
        String pin = getArguments().getString(CHECK_PIN);
        if (pin != null && !pin.equals(getText())){
            return getString(R.string.wrong_pin);
        } else {
            Bundle extras = getExtras();
            if (getTargetFragment() instanceof SimpleInputDialog.InputValidator) {
                return ((SimpleInputDialog.InputValidator) getTargetFragment())
                        .validate(getTag(), input, extras);
            }
            if (getActivity() instanceof SimpleInputDialog.InputValidator) {
                return ((SimpleInputDialog.InputValidator) getActivity())
                        .validate(getTag(), input, extras);
            }
            return null;
        }
    }



    /**
     * @return the current text or null
     */
    @Nullable
    public String getText(){
        return mInput.getText() != null ? mInput.getText().toString() : null;
    }

    /**
     * Helper for opening the soft keyboard
     */
    public void openKeyboard(){
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public View onCreateContentView(Bundle savedInstanceState) {
        // inflate and set your custom view here
        View view = inflate(R.layout.simpledialogfragment_pin);
        mInput = (PinEntryEditText) view.findViewById(R.id.pinEntry);
        mInputLayout = (TextInputLayout) view.findViewById(R.id.inputLayout);

        mInput.setMaxLength(getArguments().getInt(LENGTH, 4));


//        // TODO: currently not supported by PinEntryEditText
//        mInput.setMask(getArgString(MASK));


        if (savedInstanceState != null) {
            mInput.setText(savedInstanceState.getString(PIN));
        }

        mInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    if (posEnabled()) {
                        pressPositiveButton();
                    }
                    return true;
                }
                return false;
            }
        });

        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mInputLayout.setErrorEnabled(false);
                setPositiveButtonEnabled(posEnabled());
            }
        });

        mInput.setOnPinEnteredListener(new PinEntryEditText.OnPinEnteredListener() {
            @Override
            public void onPinEntered(CharSequence str) {
                pressPositiveButton();
            }
        });

        return view;
    }

    protected boolean posEnabled(){
        return getText() != null && getText().length() == getArguments().getInt(LENGTH, 4);
    }





    @Override
    protected void onDialogShown() {
        setPositiveButtonEnabled(posEnabled());
        mInput.requestFocus();
        openKeyboard();
    }

    @Override
    protected boolean acceptsPositiveButtonPress() {
        String input = getText();
        String error = onValidateInput(input);
        if (error == null) {
            return true;
        } else {
            mInputLayout.setError(error);
            mInputLayout.setErrorEnabled(true);
            return false;
        }
    }


    @Override
    public Bundle onResult(int which) {
        Bundle result = new Bundle();
        result.putString(PIN, getText());
        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PIN, getText());
    }
}
