package com.codepipes.ting.simpledialogfragment.form;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

import com.codepipes.ting.R;
import com.codepipes.ting.simpledialogfragment.SimpleDateDialog;
import com.codepipes.ting.simpledialogfragment.SimpleDialog;
import com.codepipes.ting.simpledialogfragment.SimpleTimeDialog;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


class DateTimeViewHolder extends FormElementViewHolder<DateTime> implements SimpleDialog.OnDialogResultListener {

    protected static final String SAVED_DATE = "date",
            SAVED_HOUR = "hour",
            SAVED_MINUTE = "minute";
    private static final String DATE_DIALOG_TAG = "datePickerDialogTag",
            TIME_DIALOG_TAG = "timePickerDialogTag";
    private EditText date, time;
    private TextInputLayout dateLayout, timeLayout;
    private Long day;
    private Integer hour, minute;

    public DateTimeViewHolder(DateTime field) {
        super(field);
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.simpledialogfragment_form_item_datetime;
    }

    @Override
    protected void setUpView(View view, final Context context, Bundle savedInstanceState,
                             final SimpleFormDialog.DialogActions actions) {

        date = (EditText) view.findViewById(R.id.date);
        time = (EditText) view.findViewById(R.id.time);
        dateLayout = (TextInputLayout) view.findViewById(R.id.dateLayout);
        timeLayout = (TextInputLayout) view.findViewById(R.id.timeLayout);

        // Label
        String text = field.getText(context);
        if (text != null){
            dateLayout.setHint(text);
            if (field.type == DateTime.Type.TIME){
                timeLayout.setHint(text);
            }
        }

        dateLayout.setVisibility(field.type == DateTime.Type.DATETIME
                || field.type == DateTime.Type.DATE ? View.VISIBLE : View.GONE);

        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateDialog dialog = SimpleDateDialog.build()
                        .title(field.getText(context))
                        .neut();
                if (field.min != null) dialog.minDate(field.min);
                if (field.max != null) dialog.maxDate(field.max);
                if (!field.required) dialog.neg(R.string.clear);
                if (day != null) dialog.date(day);
                actions.showDialog(dialog, DATE_DIALOG_TAG+field.resultKey);
            }
        });


        timeLayout.setVisibility(field.type == DateTime.Type.DATETIME
                || field.type == DateTime.Type.TIME ? View.VISIBLE : View.GONE);

        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleTimeDialog dialog = SimpleTimeDialog.build()
                                .title(field.getText(context))
                                .neut();
                if (hour != null) dialog.hour(hour);
                if (minute != null) dialog.minute(minute);
                if (!field.required) dialog.neg(R.string.clear);
                actions.showDialog(dialog, TIME_DIALOG_TAG+field.resultKey);
            }
        });

        // preset
        if (savedInstanceState != null){
            day = savedInstanceState.getLong(SAVED_DATE);
            hour = savedInstanceState.getInt(SAVED_HOUR);
            minute = savedInstanceState.getInt(SAVED_MINUTE);
        } else {
            day = field.date;
            hour = field.hour;
            minute = field.minute;
        }

        updateText();

    }

    private void updateText(){
        date.setText(day == null ? null : SimpleDateFormat.getDateInstance().format(new Date(day)));
        time.setText(hour == null || minute == null ? null :
                SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(0, 0, 0, hour, minute)));

    }


    @Override
    protected void saveState(Bundle outState) {
        if (day != null) outState.putLong(SAVED_DATE, day);
        if (hour != null) outState.putInt(SAVED_HOUR, hour);
        if (minute != null) outState.putInt(SAVED_MINUTE, minute);
    }


    @Override
    protected void putResults(Bundle results, String key) {
        Date res = new Date(day == null ? 0 : day);
        res.setHours(hour == null ? 0 : hour);
        res.setMinutes(minute == null ? 0 : minute);
        results.putLong(key, res.getTime());
    }


    @Override
    protected boolean focus(final SimpleFormDialog.FocusActions actions) {
        actions.hideKeyboard();
        if (field.type == DateTime.Type.TIME){
            time.performClick();
        } else {
            date.performClick();
        }
        return true;
    }


    @Override
    protected boolean posButtonEnabled(Context context) {
        return !field.required
                || field.type == DateTime.Type.DATE && day != null
                || field.type == DateTime.Type.TIME && hour != null && minute != null
                || field.type == DateTime.Type.DATETIME && day != null && hour != null && minute != null;
    }


    @Override
    protected boolean validate(Context context) {
        boolean valid = posButtonEnabled(context);
        dateLayout.setErrorEnabled(false);
        timeLayout.setErrorEnabled(false);
        if (!valid) {
            if (day == null){
                dateLayout.setError(context.getString(R.string.required));
            }
            if (hour == null || minute == null){
                timeLayout.setError(context.getString(R.string.required));
            }
        }
        return valid;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if ((DATE_DIALOG_TAG+field.resultKey).equals(dialogTag)){
            if (which == BUTTON_POSITIVE){
                day = extras.getLong(SimpleDateDialog.DATE);
                dateLayout.setErrorEnabled(false);
                if (field.type == DateTime.Type.DATETIME){
                    time.performClick();
                }
            } else if (which == BUTTON_NEGATIVE){
                day = null;
            }
            updateText();
            return true;
        }
        if ((TIME_DIALOG_TAG+field.resultKey).equals(dialogTag)){
            if (which == BUTTON_POSITIVE){
                hour = extras.getInt(SimpleTimeDialog.HOUR);
                minute = extras.getInt(SimpleTimeDialog.MINUTE);
                timeLayout.setErrorEnabled(false);
            } else if (which == BUTTON_NEGATIVE){
                hour = null;
                minute = null;
            }
            updateText();
            return true;
        }
        return false;
    }
}
