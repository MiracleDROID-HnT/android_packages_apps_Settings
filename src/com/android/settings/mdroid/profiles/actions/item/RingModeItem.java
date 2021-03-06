/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.mdroid.profiles.actions.item;

import com.android.settings.R;
import com.android.settings.mdroid.profiles.actions.ItemListAdapter;

import com.android.internal.util.lineageos.profiles.RingModeSettings;

public class RingModeItem extends BaseItem {
    RingModeSettings mSettings;

    public RingModeItem(RingModeSettings ringModeSettings) {
        if (ringModeSettings == null) {
            ringModeSettings = new RingModeSettings();
        }
        mSettings = ringModeSettings;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.RINGMODE_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getTitle() {
        return getString(R.string.sp_ring_mode_title);
    }

    @Override
    public String getSummary() {
        return getString(getModeString(mSettings));
    }

    public static int getModeString(RingModeSettings settings) {
        if (settings == null) {
            return R.string.sp_ring_mode_normal;
        }
        if (settings.isOverride()) {
            if (settings.getValue().equals("vibrate")) {
                return R.string.sp_ring_mode_vibrate;
            } else if (settings.getValue().equals("normal")) {
                return R.string.sp_ring_mode_normal;
            } else {
                return R.string.sp_ring_mode_mute;
            }
        } else {
            return R.string.sp_profile_action_none; //"leave unchanged"
        }
    }

    public RingModeSettings getSettings() {
        return mSettings;
    }
}