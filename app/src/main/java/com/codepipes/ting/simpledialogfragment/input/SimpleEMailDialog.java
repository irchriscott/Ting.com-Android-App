package com.codepipes.ting.simpledialogfragment.input;

import android.support.annotation.Nullable;
import android.text.InputType;

import com.codepipes.ting.R;

import java.util.regex.Pattern;

public class SimpleEMailDialog extends SimpleInputDialog {

    public static final String TAG = "SimpleEMailDialog.";

    public static final String
            EMAIL = TEXT;


    public static SimpleEMailDialog build() {
        return new SimpleEMailDialog();
    }


    protected static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    protected final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    public SimpleEMailDialog(){
        inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        hint(R.string.email_address);
    }

    @Override
    protected String onValidateInput(@Nullable String input) {
        if (input != null && pattern.matcher(input).matches()){
            return super.onValidateInput(input);
        } else {
            return getString(R.string.invalid_email_address);
        }
    }
}
