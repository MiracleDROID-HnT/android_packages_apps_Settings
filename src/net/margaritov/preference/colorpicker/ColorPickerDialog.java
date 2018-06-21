/*
 * Copyright (C) 2010 Daniel Nilsson
 * Copyright (C) 2013 Slimroms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.settings.R;

public class ColorPickerDialog extends AlertDialog implements ColorPickerView.OnColorChangedListener, View.OnClickListener {

    private final static int RECOMMENDED_COLORS_COLUMN_NUM = 4;

    private ColorPickerView mColorPicker;
    private ColorPickerPanelView mOldColor;
    private ColorPickerPanelView mNewColor;
    private EditText mHex;

    private boolean mShowLedPreview;

    private NotificationManager mNoMan;
    private Context mContext;

    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    ColorPickerDialog(Context context, int initialColor, boolean showLedPreview) {
        super(context);
        mContext = context;
        mShowLedPreview = showLedPreview;
        init(initialColor);
    }

    private void init(int color) {
        if (getWindow() != null) {
            getWindow().setFormat(PixelFormat.RGBA_8888);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setUp(color);
        }
    }

    private void setUp(int color) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mNoMan = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        assert inflater != null;
        View layout = inflater.inflate(R.layout.dui_dialog_color_picker, null);

        mColorPicker = layout.findViewById(R.id.color_picker_view);
        mOldColor = layout.findViewById(R.id.old_color_panel);
        mNewColor = layout.findViewById(R.id.new_color_panel);

        mHex = layout.findViewById(R.id.hex);
        ImageButton mSetButton = layout.findViewById(R.id.enter);

        ((LinearLayout) mOldColor.getParent()).setPadding(Math.round(mColorPicker.getDrawingOffset()),
                0, Math.round(mColorPicker.getDrawingOffset()), 0);

        mOldColor.setOnClickListener(this);
        mNewColor.setOnClickListener(this);
        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(color);
        mColorPicker.setColor(color, true);
        showLed(color);

        String[] colors = getContext().getResources().getStringArray(R.array.led_color_picker_dialog_colors);
        LinearLayout baseView = (LinearLayout)layout.findViewById(R.id.base_view);
        setUpColorButtons(colors, baseView);

        if (mHex != null) {
            mHex.setText(ColorPickerPreference.convertToARGB(color));
        }
        if (mSetButton != null) {
            mSetButton.setOnClickListener(v -> {
                String text = mHex.getText().toString();
                try {
                    int newColor = ColorPickerPreference.convertToColorInt(text);
                    mColorPicker.setColor(newColor, true);
                } catch (Exception ignored) {
                }
            });
        }

        setView(layout);
    }

    @Override
    public void onColorChanged(int color) {

        mNewColor.setColor(color);
        try {
            if (mHex != null) {
                mHex.setText(ColorPickerPreference.convertToARGB(color));
            }
        } catch (Exception ignored) {
        }
        showLed(color);
    }

    private LinearLayout createRecommendedColorsLayout(LinearLayout baseView) {
        return (LinearLayout) baseView.findViewById(R.id.recommended_colors);
    }

    private LinearLayout createRowColorsLayout() {
        LinearLayout rowColorsLayout = new LinearLayout(getContext());

        rowColorsLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowColorsLayout.setWeightSum(100.0f);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        rowColorsLayout.setLayoutParams(layoutParams);

        return rowColorsLayout;
    }

    private Button createColorButton(String color) {
        Button button = new Button(getContext());

        button.setBackgroundColor(Color.parseColor("#" + color));

        final float scale = getContext().getResources().getDisplayMetrics().density;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, (int)(50 * scale + 0.5f), 25.0f);
        //50 - hight in dp, 25.0f - % filling with one button (25 - 4 buttons, must divide 100 without residue)
        button.setLayoutParams(params);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int newColor = ColorPickerPreference.convertToColorInt(color);
                    mColorPicker.setColor(newColor, true);
                } catch (Exception ignored) {
                }
            }
        });

        return button;
    }

    private void setUpColorButtons(String[] colors, LinearLayout baseView) {
        LinearLayout recommendedColorsLayout = createRecommendedColorsLayout(baseView);

        int columns_num = RECOMMENDED_COLORS_COLUMN_NUM;
        int rows_num    = (colors.length + columns_num - 1) / columns_num;

        for (int row = 0; row != rows_num; row++) {
            LinearLayout rowLayout = createRowColorsLayout();
            recommendedColorsLayout.addView(rowLayout);

            for (int column = 0; column != columns_num; column++) {
                int colorIndex = (row * columns_num) + column;
                if (colorIndex >= colors.length) break;
                Button button = createColorButton(colors[colorIndex]);
                rowLayout.addView(button);
            }
        }
    }

    private void showLed(int color) {
        if (mShowLedPreview) {
            if (color == 0xFFFFFFFF) {
                // argb white doesn't work
                color = 0xffffff;
            }
            mNoMan.forceShowLedLight(color);
        }
    }

    private void switchOffLed() {
        if (mShowLedPreview) {
            mNoMan.forceShowLedLight(-1);
        }
    }

    void setAlphaSliderVisible(boolean visible) {
        mColorPicker.setAlphaSliderVisible(visible);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color selected by the user has changed.
     *
     * @param listener
     */
    void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.new_color_panel) {
            if (mListener != null) {
                mListener.onColorChanged(mNewColor.getColor());
            }
        }
        dismiss();
        switchOffLed();
    }

    @Override
    public void onStop() {
        super.onStop();
        switchOffLed();
    }


    @NonNull
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("old_color", mOldColor.getColor());
        state.putInt("new_color", mNewColor.getColor());
        dismiss();
        switchOffLed();
        return state;
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOldColor.setColor(savedInstanceState.getInt("old_color"));
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true);
    }
}
