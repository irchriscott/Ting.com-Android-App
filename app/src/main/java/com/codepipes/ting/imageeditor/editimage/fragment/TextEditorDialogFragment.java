package com.codepipes.ting.imageeditor.editimage.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.codepipes.ting.R;
import com.codepipes.ting.imageeditor.editimage.adapter.ColorPickerAdapter;
import com.codepipes.ting.imageeditor.editimage.interfaces.OnTextEditorListener;

import org.jetbrains.annotations.NotNull;

public class TextEditorDialogFragment extends DialogFragment {

    public static final String TAG = TextEditorDialogFragment.class.getSimpleName();
    private static final String EXTRA_INPUT_TEXT = "extra_input_text";
    private static final String EXTRA_COLOR_CODE = "extra_color_code";

    private EditText addTextEditText;
    private InputMethodManager inputMethodManager;
    private int colorCode;
    private OnTextEditorListener onTextEditorListener;

    //Show dialog with provide text and text color
    public static TextEditorDialogFragment show(@NonNull AppCompatActivity appCompatActivity,
                                                @NonNull String inputText,
                                                @ColorInt int initialColorCode) {
        Bundle args = new Bundle();
        args.putString(EXTRA_INPUT_TEXT, inputText);
        args.putInt(EXTRA_COLOR_CODE, initialColorCode);
        TextEditorDialogFragment fragment = new TextEditorDialogFragment();
        fragment.setArguments(args);
        fragment.show(appCompatActivity.getSupportFragmentManager(), TAG);
        return fragment;
    }

    //Show dialog with default text input as empty and text color white
    public static TextEditorDialogFragment show(@NonNull AppCompatActivity appCompatActivity) {
        return show(appCompatActivity, "", ContextCompat.getColor(appCompatActivity, R.color.colorWhite));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        //Make dialog full screen with transparent background
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if (window != null) {
                dialog.getWindow().setLayout(width, height);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_editor_dialog_edit_text_sticker, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addTextEditText = view.findViewById(R.id.add_text_edit_text);
        inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        TextView addTextDoneTv = view.findViewById(R.id.add_text_done_tv);

        //Setup the color picker for text color
        RecyclerView addTextColorPickerRecyclerView = view.findViewById(R.id.add_text_color_picker_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        addTextColorPickerRecyclerView.setLayoutManager(layoutManager);
        addTextColorPickerRecyclerView.setHasFixedSize(true);
        ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(getContext());

        //This listener will change the text color when clicked on any color from picker
        colorPickerAdapter.setOnColorPickerClickListener(colorCode -> {
            this.colorCode = colorCode;
            addTextEditText.setTextColor(colorCode);
        });

        addTextColorPickerRecyclerView.setAdapter(colorPickerAdapter);
        addTextEditText.setText(getArguments().getString(EXTRA_INPUT_TEXT));
        colorCode = getArguments().getInt(EXTRA_COLOR_CODE);
        addTextEditText.setTextColor(colorCode);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        //Make a callback on activity when user is done with text editing
        addTextDoneTv.setOnClickListener(view1 -> {
            inputMethodManager.hideSoftInputFromWindow(view1.getWindowToken(), 0);

            String inputText = addTextEditText.getText().toString();
            if (!TextUtils.isEmpty(inputText) && onTextEditorListener != null) {
                onTextEditorListener.onDone(inputText, colorCode);
            }
            dismiss();
        });
    }

    //Callback to listener if user is done with text editing
    public void setOnTextEditorListener(OnTextEditorListener onTextEditorListener) {
        this.onTextEditorListener = onTextEditorListener;
    }
}
