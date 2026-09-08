/*
 * Copyright (C) 2026 Lunaris AOSP
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
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class BatteryInfoUtils {
    private static final String TAG = "BatteryInfoUtils";

    private BatteryInfoUtils() {}

    public static String readNode(Context context, int resId) {
        if (context == null || resId == 0) {
            return null;
        }
        try {
            String path = context.getString(resId);
            return readSysfs(path);
        } catch (Exception e) {
            return null;
        }
    }

    public static String readSysfs(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line != null ? line.trim() : null;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read node: " + path, e);
            return null;
        }
    }

    public static int readIntNode(Context context, int resId, int defaultValue) {
        String val = readNode(context, resId);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static long readLongNode(Context context, int resId, long defaultValue) {
        String val = readNode(context, resId);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static boolean isNodeValid(Context context, int resId) {
        if (context == null || resId == 0) {
            return false;
        }
        try {
            String path = context.getString(resId);
            if (TextUtils.isEmpty(path)) {
                return false;
            }
            File file = new File(path);
            return file.exists() && file.canRead();
        } catch (Exception e) {
            return false;
        }
    }
}
