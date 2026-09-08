/*
 * Copyright (C) 2024 Paranoid Android
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
import android.content.Intent;
import android.os.BatteryManager;

import com.android.internal.os.PowerProfile;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.fuelgauge.BatteryUtils;

/**
 * A controller that manages the information about battery maximum capacity.
 */
public class BatteryMaximumCapacityPreferenceController extends BasePreferenceController {

    public BatteryMaximumCapacityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (BatteryInfoUtils.isNodeValid(mContext, R.string.config_battery_maximum_capacity_node)) {
            return AVAILABLE;
        }
        boolean isFeatureEnabled = mContext.getResources().getBoolean(R.bool.config_show_battery_maximum_capacity);
        return isFeatureEnabled ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        int maxCapacityUah = BatteryInfoUtils.readIntNode(
                mContext, R.string.config_battery_maximum_capacity_node, -1);
        if (maxCapacityUah <= 0) {
            Intent batteryIntent = BatteryUtils.getBatteryIntent(mContext);
            maxCapacityUah = batteryIntent.getIntExtra(BatteryManager.EXTRA_MAXIMUM_CAPACITY, -1);
        }

        int designCapacityUah = BatteryInfoUtils.readIntNode(
                mContext, R.string.config_battery_design_capacity_node, -1);
        if (designCapacityUah <= 0) {
            boolean usePowerProfileFirst = mContext.getResources().getBoolean(R.bool.config_use_power_profile_for_battery_capacity);
            if (usePowerProfileFirst) {
                final PowerProfile profile = new PowerProfile(mContext);
                designCapacityUah = (int) profile.getBatteryCapacity() * 1000;
                if (designCapacityUah <= 0) {
                    Intent batteryIntent = BatteryUtils.getBatteryIntent(mContext);
                    designCapacityUah = batteryIntent.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1);
                }
            } else {
                Intent batteryIntent = BatteryUtils.getBatteryIntent(mContext);
                designCapacityUah = batteryIntent.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1);
                if (designCapacityUah <= 0) {
                    final PowerProfile profile = new PowerProfile(mContext);
                    designCapacityUah = (int) profile.getBatteryCapacity() * 1000;
                }
            }
        }

        if (maxCapacityUah > 0 && designCapacityUah > 0) {
            int maxCapacity = maxCapacityUah > 100_000 ? maxCapacityUah / 1_000 : maxCapacityUah;
            int designCapacity = designCapacityUah > 100_000 ? designCapacityUah / 1_000 : designCapacityUah;
            int percentage = (maxCapacity * 100) / designCapacity;

            return mContext.getString(
                    R.string.battery_maximum_capacity_summary, maxCapacity, percentage);
        }

        return mContext.getString(R.string.battery_maximum_capacity_not_available);
    }
}