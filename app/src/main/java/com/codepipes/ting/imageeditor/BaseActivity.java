package com.codepipes.ting.imageeditor;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.codepipes.ting.dialogs.messages.ProgressOverlay;

public class BaseActivity extends AppCompatActivity {

    public static ProgressOverlay getLoadingDialog(Context context, int titleId,
                                          boolean canCancel) {
        return getLoadingDialog(context, context.getString(titleId), canCancel);
    }


    public static ProgressOverlay getLoadingDialog(Context context, String title,
                                          boolean canCancel) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setCancelable(canCancel);
        dialog.setMessage(title);

        ProgressOverlay progressOverlay = new ProgressOverlay();
        progressOverlay.setCancelable(canCancel);

        return progressOverlay;
    }
}
