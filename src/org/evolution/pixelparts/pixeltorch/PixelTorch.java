/*
 * Copyright (C) 2023 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts.pixeltorch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.evolution.pixelparts.Constants;
import org.evolution.pixelparts.R;

public class PixelTorch extends PreferenceFragment implements OnMainSwitchChangeListener {
    private static final String TAG = PixelTorch.class.getSimpleName();

    private MainSwitchPreference mPixelTorchSwitch;

    private static final String[] PIXEL_TORCH_PREFERENCES = new String[] {
            Constants.KEY_PIXEL_TORCH_STRENGTH,
            Constants.KEY_SHAKE_SENSITIVITY,
            Constants.KEY_SHAKE_THRESHOLD
    };

    private static boolean mPixelTorchServiceEnabled = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pixeltorch);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mPixelTorchSwitch = findPreference(Constants.KEY_PIXEL_TORCH);
        mPixelTorchSwitch.setChecked(sharedPrefs.getBoolean(Constants.KEY_PIXEL_TORCH, false));
        mPixelTorchSwitch.addOnSwitchChangeListener(this);
        togglePixelTorchPreferencesVisibility(mPixelTorchSwitch.isChecked());
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        prefChange.putBoolean(Constants.KEY_PIXEL_TORCH, isChecked).apply();
        togglePixelTorchService(getContext());
        togglePixelTorchPreferencesVisibility(isChecked);
    }

    private void togglePixelTorchPreferencesVisibility(boolean isEnabled) {
        for (String prefKey : PIXEL_TORCH_PREFERENCES) {
            Preference pref = findPreference(prefKey);
            if (pref != null) {
                pref.setVisible(isEnabled);
            }
        }
    }

    public static void togglePixelTorchService(Context context) {
        boolean isPixelTorchEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_PIXEL_TORCH, false);

        if (isPixelTorchEnabled && !mPixelTorchServiceEnabled) {
            startPixelTorchService(context);
        } else if (!isPixelTorchEnabled && mPixelTorchServiceEnabled) {
            stopPixelTorchService(context);
        }
    }

    private static void startPixelTorchService(Context context) {
        context.startServiceAsUser(new Intent(context, PixelTorchService.class),
                UserHandle.CURRENT);
        mPixelTorchServiceEnabled = true;
    }

    private static void stopPixelTorchService(Context context) {
        mPixelTorchServiceEnabled = false;
        context.stopServiceAsUser(new Intent(context, PixelTorchService.class),
                UserHandle.CURRENT);
    }
}
