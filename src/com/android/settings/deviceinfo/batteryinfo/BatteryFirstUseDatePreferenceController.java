/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.deviceinfo.batteryinfo;

import android.content.Context;
import android.os.BatteryManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;

import java.util.concurrent.TimeUnit;

/**
 * A controller that manages the information about battery first use date.
 */
public class BatteryFirstUseDatePreferenceController extends BasePreferenceController {

    private final BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;
    private final BatteryManager mBatteryManager;

    private long mFirstUseDateInMs;

    public BatteryFirstUseDatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatterySettingsFeatureProvider = FeatureFactory.getFeatureFactory()
                .getBatterySettingsFeatureProvider();
        mBatteryManager = mContext.getSystemService(BatteryManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if (BatteryInfoUtils.isNodeValid(mContext, R.string.config_battery_first_use_date_node)) {
            return getFirstUseDate() > 0 ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
        return mBatterySettingsFeatureProvider.isFirstUseDateAvailable(mContext, getFirstUseDate())
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return isAvailable()
                ? BatteryUtils.getBatteryInfoFormattedDate(mFirstUseDateInMs)
                : null;
    }

    private long getFirstUseDate() {
        if (mFirstUseDateInMs == 0L) {
            long customDate = BatteryInfoUtils.readLongNode(
                    mContext, R.string.config_battery_first_use_date_node, 0L);
            if (customDate > 0L) {
                mFirstUseDateInMs = customDate > 1_000_000_000_000L
                        ? customDate
                        : TimeUnit.MILLISECONDS.convert(customDate, TimeUnit.SECONDS);
            } else {
                final long firstUseDateInSec = mBatteryManager.getLongProperty(
                        BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE);
                mFirstUseDateInMs = TimeUnit.MILLISECONDS.convert(firstUseDateInSec, TimeUnit.SECONDS);
            }
        }
        return mFirstUseDateInMs;
    }
}
