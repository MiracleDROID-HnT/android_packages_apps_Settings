/*
 * Copyright (C) 2011 Sergey Margaritov
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2015 The TeamEos Project
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.*;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.GridLayout;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.settings.R;

/**
 * A preference type that allows a user to choose a time
 *
 * @author Sergey Margaritov
 */
public class ColorPickerPreference extends Preference implements
        Preference.OnPreferenceClickListener, ColorPickerDialog.OnColorChangedListener {

    PreferenceViewHolder mView;
    ColorPickerDialog mDialog;
    AlertDialog mSimpleDialog;
    LinearLayout widgetFrameView;
    private int mValue = Color.BLACK;
    private float mDensity = 0;
    private boolean mAlphaSliderEnabled = false;
    private boolean mEnabled = true;

    // if we return -6, button is not enabled
    static final String SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings";
    static final int DEF_VALUE_DEFAULT = -6;
    boolean mUsesDefaultButton = false;
    int mDefValue = -1;

    private boolean mShowLedPreview;
    private boolean mIsLedColorPicker;

    private EditText mEditText;

    private boolean mIsCrappyLedDevice;

    public ColorPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, Color.BLACK);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        onColorChanged(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    private void init(Context context, AttributeSet attrs) {
        mIsCrappyLedDevice = getContext().getResources().getBoolean(com.android.internal.R.bool.config_isCrappyLedDevice);
        mDensity = getContext().getResources().getDisplayMetrics().density;
        setOnPreferenceClickListener(this);
        if (attrs != null) {
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", false);
            int defVal = attrs.getAttributeIntValue(SETTINGS_NS, "defaultColorValue", DEF_VALUE_DEFAULT);
            if (defVal != DEF_VALUE_DEFAULT) {
                mUsesDefaultButton =  true;
                mDefValue = defVal;
            }
            mShowLedPreview = attrs.getAttributeBooleanValue(null, "ledPreview", false);
            mIsLedColorPicker = attrs.getAttributeBooleanValue(null, "isledPicker", false);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        mView = view;
        super.onBindViewHolder(view);

        view.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(null);
            }
        });

        widgetFrameView = ((LinearLayout) view
                .findViewById(android.R.id.widget_frame));

        if (mUsesDefaultButton) {
            setDefaultButton();
        }

        setPreviewColor();
    }

    /**
     * Restore a default value, not necessarily a color
     * For example: Set default value to -1 to remove a color filter
     *
     * @author Randall Rushing aka Bigrushdog
     */
    private void setDefaultButton() {
        if (mView == null)
            return;

        LinearLayout widgetFrameView = ((LinearLayout) mView
                .findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null)
            return;

        ImageView defView = new ImageView(getContext());
        widgetFrameView.setOrientation(LinearLayout.HORIZONTAL);

        // remove already created default button
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            View oldView = widgetFrameView.findViewWithTag("default");
            View spacer = widgetFrameView.findViewWithTag("spacer");
            if (oldView != null) {
                widgetFrameView.removeView(oldView);
            }
            if (spacer != null) {
                widgetFrameView.removeView(spacer);
            }
        }

        widgetFrameView.addView(defView);
        widgetFrameView.setMinimumWidth(0);
        defView.setBackground(getContext().getDrawable(R.drawable.ic_settings_backup_restore));
        defView.setTag("default");
        defView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getOnPreferenceChangeListener().onPreferenceChange(ColorPickerPreference.this,
                            Integer.valueOf(mDefValue));
                    onColorChanged(mDefValue);
                } catch (NullPointerException e) {
                }
            }
        });

        // sorcery for a linear layout ugh
        View spacer = new View(getContext());
        spacer.setTag("spacer");
        spacer.setLayoutParams(new LinearLayout.LayoutParams((int) (mDensity * 16),
                LayoutParams.MATCH_PARENT));
        widgetFrameView.addView(spacer);
    }

    private void setPreviewColor() {
        if (mView == null)
            return;

        ImageView iView = new ImageView(getContext());
        LinearLayout widgetFrameView = ((LinearLayout) mView
                .findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null)
            return;

        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                (int) (mDensity * 8),
                widgetFrameView.getPaddingBottom()
                );
        // remove already create preview image
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            View preview = widgetFrameView.findViewWithTag("preview");
            if (preview != null) {
                widgetFrameView.removeView(preview);
            }
        }
        widgetFrameView.addView(iView);
        widgetFrameView.setMinimumWidth(0);
        final int size = (int) getContext().getResources().getDimension(R.dimen.oval_notification_size);
        final int imageColor = ((mValue & 0xF0F0F0) == 0xF0F0F0) ?
                (mValue - 0x101010) : mValue;
        iView.setImageDrawable(createOvalShape(size, 0xFF000000 + imageColor));
        iView.setTag("preview");
        iView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEnabled) {
                    showDialog(null);
                }
            }
        });
    }

    private static ShapeDrawable createOvalShape(int size, int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEnabled != enabled) {
            mEnabled = enabled;
        }
    }

    @Override
    public void onColorChanged(int color) {
        if (isPersistent()) {
            persistInt(color);
        }
        mValue = color;
        setPreviewColor();
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {
        }
        try {
            mEditText.setText(Integer.toString(color, 16));
        } catch (NullPointerException e) {
        }
    }

    private void onSimpleColorChanged(int color) {
        onColorChanged(color);
    }

    public boolean onPreferenceClick(Preference preference) {
        //showDialog(null);
        return false;
    }

    protected void showDialog(Bundle state) {
        if (mIsCrappyLedDevice && (mShowLedPreview || mIsLedColorPicker)) {
            showSimplePickerDialog();
            return;
        }

        mDialog = new ColorPickerDialog(getContext(), mValue, mShowLedPreview);
        mDialog.setOnColorChangedListener(this);
        if (mAlphaSliderEnabled) {
            mDialog.setAlphaSliderVisible(true);
        }
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
        mDialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void showSimplePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.color_picker_simple_dialog, null);
        builder.setView(mView)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSimpleDialog.dismiss();
                        }
                })
                .setNeutralButton(R.string.color_default, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onSimpleColorChanged(mDefValue != 1 ? mDefValue : Color.WHITE);
                        }
                })
                .setCancelable(false);

        Button color_1 = null;
        if (mView != null) {
            color_1 = mView.findViewById(R.id.color_1);
        }
        if (color_1 != null) {
            color_1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_1));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_2 = null;
        if (mView != null) {
            color_2 = mView.findViewById(R.id.color_2);
        }
        if (color_2 != null) {
            color_2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_2));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_3 = null;
        if (mView != null) {
            color_3 = mView.findViewById(R.id.color_3);
        }
        if (color_3 != null) {
            color_3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_3));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_4 = null;
        if (mView != null) {
            color_4 = mView.findViewById(R.id.color_4);
        }
        if (color_4 != null) {
            color_4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_4));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_5 = null;
        if (mView != null) {
            color_5 = mView.findViewById(R.id.color_5);
        }
        if (color_5 != null) {
            color_5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_5));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_6 = null;
        if (mView != null) {
            color_6 = mView.findViewById(R.id.color_6);
        }
        if (color_6 != null) {
            color_6.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_6));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_7 = null;
        if (mView != null) {
            color_7 = mView.findViewById(R.id.color_7);
        }
        if (color_7 != null) {
            color_7.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_7));
                    mSimpleDialog.dismiss();
                }
            });
        }
        Button color_8 = null;
        if (mView != null) {
            color_8 = mView.findViewById(R.id.color_8);
        }
        if (color_8 != null) {
            color_8.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSimpleColorChanged(getContext().getColor(R.color.simple_color_8));
                    mSimpleDialog.dismiss();
                }
            });
        }

        GridLayout gridlayout;
        if (mView != null) {
            int intOrientation = getContext().getResources().getConfiguration().orientation;
            // Lets split this up instead of creating two different layouts
            // just so we can change the columns
            gridlayout = mView.findViewById(R.id.Gridlayout);
            if (intOrientation == Configuration.ORIENTATION_PORTRAIT) {
                gridlayout.setColumnCount(4);
            } else {
                gridlayout.setColumnCount(8);
            }
        }

        mSimpleDialog = builder.create();
        mSimpleDialog.show();

    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     *
     * @param enable
     */
    public void setAlphaSliderEnabled(boolean enable) {
        mAlphaSliderEnabled = enable;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * set color preview value from outside
     * @author kufikugel
     */
    public void setNewPreviewColor(int color) {
        onColorChanged(color);
    }

    public void setDefaultColor(int color) {
        mDefValue = color;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * @param color
     * @author Unknown
     */
    public static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + green + blue;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * @param argb
     * @throws NumberFormatException
     * @author Unknown
     */
    public static int convertToColorInt(String argb) throws NumberFormatException {

        if (argb.startsWith("#")) {
            argb = argb.replace("#", "");
        }

        int alpha = -1, red = -1, green = -1, blue = -1;

        if (argb.length() == 8) {
            alpha = Integer.parseInt(argb.substring(0, 2), 16);
            red = Integer.parseInt(argb.substring(2, 4), 16);
            green = Integer.parseInt(argb.substring(4, 6), 16);
            blue = Integer.parseInt(argb.substring(6, 8), 16);
        }
        else if (argb.length() == 6) {
            alpha = 255;
            red = Integer.parseInt(argb.substring(0, 2), 16);
            green = Integer.parseInt(argb.substring(2, 4), 16);
            blue = Integer.parseInt(argb.substring(4, 6), 16);
        }

        return Color.argb(alpha, red, green, blue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mIsCrappyLedDevice && (mShowLedPreview || mIsLedColorPicker)) {
            if (mSimpleDialog == null || !mSimpleDialog.isShowing()) {
                return superState;
            }
            final SavedState myState = new SavedState(superState);
            myState.dialogBundle = mSimpleDialog.onSaveInstanceState();
            return myState;
        } else {
            if (mDialog == null || !mDialog.isShowing()) {
                return superState;
            }
            final SavedState myState = new SavedState(superState);
            myState.dialogBundle = mDialog.onSaveInstanceState();
            return myState;
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof SavedState)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        showDialog(myState.dialogBundle);
    }

    private static class SavedState extends BaseSavedState {
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
