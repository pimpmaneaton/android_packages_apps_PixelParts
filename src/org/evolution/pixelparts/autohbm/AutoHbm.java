/*
 * Copyright (C) 2023 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts.autohbm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;
import com.android.settingslib.widget.UsageProgressBarPreference;

import org.evolution.pixelparts.Constants;
import org.evolution.pixelparts.CustomSeekBarPreference;
import org.evolution.pixelparts.R;

public class AutoHbm extends PreferenceFragment
        implements OnMainSwitchChangeListener, SensorEventListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = AutoHbm.class.getSimpleName();

    private static final String[] AUTO_HBM_PREFERENCES = {
            Constants.KEY_AUTO_HBM_THRESHOLD,
            Constants.KEY_AUTO_HBM_ENABLE_TIME,
            Constants.KEY_AUTO_HBM_DISABLE_TIME,
            Constants.KEY_CURRENT_LUX_LEVEL
    };

    private CustomSeekBarPreference mAutoHbmThresholdPreference;
    private MainSwitchPreference mAutoHbmSwitch;
    private SensorManager mSensorManager;
    private SharedPreferences mSharedPrefs;
    private Sensor mLightSensor;
    private UsageProgressBarPreference mCurrentLuxLevelPreference;
    private int mCurrentLux;

    private static boolean mAutoHbmServiceEnabled = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.autohbm);

        Context context = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mAutoHbmSwitch = findPreference(Constants.KEY_AUTO_HBM);
        mAutoHbmSwitch.setChecked(mSharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        mAutoHbmSwitch.addOnSwitchChangeListener(this);

        mAutoHbmThresholdPreference = findPreference(Constants.KEY_AUTO_HBM_THRESHOLD);
        mAutoHbmThresholdPreference.setOnPreferenceChangeListener(this);

        mCurrentLuxLevelPreference = findPreference(Constants.KEY_CURRENT_LUX_LEVEL);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        toggleAutoHbmPreferencesVisibility(mAutoHbmSwitch.isChecked());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAutoHbmSwitch.isChecked()) {
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAutoHbmSwitch.isChecked()) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, isChecked).apply();
        toggleAutoHbmService(getContext());
        toggleAutoHbmPreferencesVisibility(isChecked);

        if (isChecked) {
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAutoHbmThresholdPreference && mCurrentLuxLevelPreference != null) {
            int threshold = (int) newValue;
            updateCurrentLuxLevelPreference(mCurrentLux, threshold);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT && mCurrentLuxLevelPreference != null) {
            float luxValue = event.values[0];
            mCurrentLux = (int) luxValue;
            int threshold = mSharedPrefs.getInt(Constants.KEY_AUTO_HBM_THRESHOLD, 20000);
            updateCurrentLuxLevelPreference(mCurrentLux, threshold);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void updateCurrentLuxLevelPreference(int currentLux, int threshold) {
        if (mCurrentLuxLevelPreference != null) {
            mCurrentLuxLevelPreference.setUsageSummary(String.valueOf(currentLux));
            mCurrentLuxLevelPreference.setTotalSummary(String.valueOf(threshold));

            if (currentLux >= threshold) {
                mCurrentLuxLevelPreference.setPercent(100, 100);
            } else {
                mCurrentLuxLevelPreference.setPercent(currentLux, threshold);
            }
        }
    }

    private void toggleAutoHbmPreferencesVisibility(boolean show) {
        for (String prefKey : AUTO_HBM_PREFERENCES) {
            Preference pref = findPreference(prefKey);
            if (pref != null) {
                pref.setVisible(show);
            }
        }
    }

    public static void toggleAutoHbmService(Context context) {
        boolean isAutoHbmEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_AUTO_HBM, false);

        if (isAutoHbmEnabled && !mAutoHbmServiceEnabled) {
            startAutoHbmService(context);
        } else if (!isAutoHbmEnabled && mAutoHbmServiceEnabled) {
            stopAutoHbmService(context);
        }
    }

    private static void startAutoHbmService(Context context) {
        context.startServiceAsUser(new Intent(context, AutoHbmService.class),
                UserHandle.CURRENT);
        mAutoHbmServiceEnabled = true;
    }

    private static void stopAutoHbmService(Context context) {
        mAutoHbmServiceEnabled = false;
        context.stopServiceAsUser(new Intent(context, AutoHbmService.class),
                UserHandle.CURRENT);
    }
}
