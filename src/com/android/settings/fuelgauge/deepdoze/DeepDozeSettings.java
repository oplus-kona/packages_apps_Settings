/*
 * Copyright (C) 2026 Project ASCP
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

package com.android.settings.fuelgauge.deepdoze;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.MainSwitchPreference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DeepDozeSettings extends SettingsPreferenceFragment implements OnCheckedChangeListener {

    private static final String TAG = "DeepDozeSettings";

    private static final String KEY_MAIN_SWITCH = "deep_doze_main_switch";
    private static final String KEY_TIMELINE = "deep_doze_timeline";
    private static final String KEY_LATENCY = "deep_doze_latency";
    private static final String KEY_DRAIN = "deep_doze_drain";
    private static final String KEY_DISTURBANCE_CAT = "deep_doze_disturbance_category";
    private static final String KEY_DISTURBANCE_EMPTY = "deep_doze_disturbance_empty";

    private MainSwitchPreference mMainSwitch;
    private Preference mTimelinePref;
    private Preference mLatencyPref;
    private Preference mDrainPref;
    private PreferenceCategory mDisturbanceCat;
    private Preference mDisturbanceEmptyPref;

    private ContentResolver mContentResolver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SettingsObserver mSettingsObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.deep_doze_settings);

        mContentResolver = getContentResolver();

        mMainSwitch = findPreference(KEY_MAIN_SWITCH);
        mTimelinePref = findPreference(KEY_TIMELINE);
        mLatencyPref = findPreference(KEY_LATENCY);
        mDrainPref = findPreference(KEY_DRAIN);
        mDisturbanceCat = findPreference(KEY_DISTURBANCE_CAT);
        mDisturbanceEmptyPref = findPreference(KEY_DISTURBANCE_EMPTY);

        if (mMainSwitch != null) {
            mMainSwitch.addOnSwitchChangeListener(this);
        }

        setupSleepPreferences();

        mSettingsObserver = new SettingsObserver(mHandler);
    }

    private void setupSleepPreferences() {
        androidx.preference.SwitchPreferenceCompat schedulePref = findPreference("deep_doze_schedule_mode");
        if (schedulePref != null) {
            schedulePref.setChecked(Settings.Secure.getIntForUser(mContentResolver,
                    Settings.Secure.DEEP_DOZE_SCHEDULE_MODE, 0, UserHandle.USER_CURRENT) == 1);
            schedulePref.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean val = (Boolean) newValue;
                return Settings.Secure.putIntForUser(mContentResolver,
                        Settings.Secure.DEEP_DOZE_SCHEDULE_MODE, val ? 1 : 0, UserHandle.USER_CURRENT);
            });
        }

        androidx.preference.SwitchPreferenceCompat warmPref = findPreference("deep_doze_pre_alarm_warming");
        if (warmPref != null) {
            warmPref.setChecked(Settings.Secure.getIntForUser(mContentResolver,
                    Settings.Secure.DEEP_DOZE_PRE_ALARM_WARMING, 1, UserHandle.USER_CURRENT) == 1);
            warmPref.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean val = (Boolean) newValue;
                return Settings.Secure.putIntForUser(mContentResolver,
                        Settings.Secure.DEEP_DOZE_PRE_ALARM_WARMING, val ? 1 : 0, UserHandle.USER_CURRENT);
            });
        }

        androidx.preference.SwitchPreferenceCompat repeatPref = findPreference("deep_doze_repeat_callers");
        if (repeatPref != null) {
            repeatPref.setChecked(Settings.Secure.getIntForUser(mContentResolver,
                    Settings.Secure.DEEP_DOZE_REPEAT_CALLERS, 1, UserHandle.USER_CURRENT) == 1);
            repeatPref.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean val = (Boolean) newValue;
                return Settings.Secure.putIntForUser(mContentResolver,
                        Settings.Secure.DEEP_DOZE_REPEAT_CALLERS, val ? 1 : 0, UserHandle.USER_CURRENT);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSettingsObserver != null) {
            mSettingsObserver.observe();
        }
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSettingsObserver != null) {
            mSettingsObserver.unobserve();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Settings.Secure.putIntForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_ENABLED, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    private void updateUi() {
        if (mContentResolver == null) {
            return;
        }

        final boolean enabled = Settings.Secure.getIntForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        final boolean isActive = Settings.Secure.getIntForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_IS_ACTIVE, 0, UserHandle.USER_CURRENT) == 1;
        final long startTime = Settings.Secure.getLongForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_START_TIME, 0L, UserHandle.USER_CURRENT);
        final long endTime = Settings.Secure.getLongForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_END_TIME, 0L, UserHandle.USER_CURRENT);
        final long latencyMs = Settings.Secure.getLongForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_ENTRY_DURATION_MS, 0L, UserHandle.USER_CURRENT);
        final int startBattery = Settings.Secure.getIntForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_START_BATTERY, -1, UserHandle.USER_CURRENT);
        final int endBattery = Settings.Secure.getIntForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_END_BATTERY, -1, UserHandle.USER_CURRENT);
        final String disturbanceJson = Settings.Secure.getStringForUser(mContentResolver,
                Settings.Secure.DEEP_DOZE_DISTURBANCE_LOG, UserHandle.USER_CURRENT);

        if (mMainSwitch != null) {
            mMainSwitch.setChecked(enabled);
        }

        if (mTimelinePref != null) {
            if (startTime == 0L) {
                mTimelinePref.setSummary("No session recorded yet");
            } else {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String fromStr = timeFormat.format(new Date(startTime));
                if (isActive) {
                    mTimelinePref.setSummary("Active now — entered at " + fromStr);
                } else if (endTime > startTime) {
                    String toStr = timeFormat.format(new Date(endTime));
                    long durationMs = endTime - startTime;
                    long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
                    String durationStr = hours > 0
                            ? String.format(Locale.getDefault(), "%dh %dm total", hours, minutes)
                            : (minutes > 0
                                ? String.format(Locale.getDefault(), "%dm %ds total", minutes, seconds)
                                : String.format(Locale.getDefault(), "%ds total", seconds));
                    mTimelinePref.setSummary("From " + fromStr + " to " + toStr + " (" + durationStr + ")");
                } else {
                    mTimelinePref.setSummary("Started at " + fromStr);
                }
            }
        }

        if (mLatencyPref != null) {
            if (latencyMs > 0) {
                if (latencyMs < 1000) {
                    mLatencyPref.setSummary(latencyMs + " ms (Instant entry)");
                } else {
                    mLatencyPref.setSummary(String.format(Locale.getDefault(), "%.2f seconds", latencyMs / 1000f));
                }
            } else {
                mLatencyPref.setSummary("Not recorded yet");
            }
        }

        if (mDrainPref != null) {
            if (startBattery >= 0 && endBattery >= 0) {
                int drained = Math.max(0, startBattery - endBattery);
                mDrainPref.setSummary(drained + "% drained (" + startBattery + "% → " + endBattery + "%)");
            } else if (startBattery >= 0) {
                mDrainPref.setSummary("Started at " + startBattery + "% (currently tracking)");
            } else {
                mDrainPref.setSummary("No data available");
            }
        }

        updateDisturbanceLog(disturbanceJson);
    }

    private void updateDisturbanceLog(String json) {
        if (mDisturbanceCat == null) {
            return;
        }

        mDisturbanceCat.removeAll();

        if (TextUtils.isEmpty(json)) {
            if (mDisturbanceEmptyPref != null) {
                mDisturbanceCat.addPreference(mDisturbanceEmptyPref);
            }
            return;
        }

        try {
            JSONArray array = new JSONArray(json);
            if (array.length() == 0) {
                if (mDisturbanceEmptyPref != null) {
                    mDisturbanceCat.addPreference(mDisturbanceEmptyPref);
                }
                return;
            }

            PackageManager pm = requireContext().getPackageManager();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String pkg = obj.optString("package", "unknown");
                String tag = obj.optString("tag", "wakelock");
                String time = obj.optString("formatted_time", "");
                String action = obj.optString("action", "Killed & Blocked");

                CharSequence label = pkg;
                Drawable icon = null;
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    label = pm.getApplicationLabel(ai);
                    icon = pm.getApplicationIcon(ai);
                } catch (PackageManager.NameNotFoundException ignored) {
                }

                Preference pref = new Preference(requireContext());
                pref.setTitle(label);
                pref.setSummary("Wake lock: \"" + tag + "\" at " + time + " • " + action);
                if (icon != null) {
                    pref.setIcon(icon);
                }
                pref.setSelectable(false);
                mDisturbanceCat.addPreference(pref);
            }
        } catch (Exception e) {
            if (mDisturbanceEmptyPref != null) {
                mDisturbanceCat.addPreference(mDisturbanceEmptyPref);
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver cr = mContentResolver;
            if (cr == null) return;
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_ENABLED),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_IS_ACTIVE),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_START_TIME),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_END_TIME),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_ENTRY_DURATION_MS),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_START_BATTERY),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_END_BATTERY),
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEEP_DOZE_DISTURBANCE_LOG),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            if (mContentResolver != null) {
                mContentResolver.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateUi();
        }
    }
}
