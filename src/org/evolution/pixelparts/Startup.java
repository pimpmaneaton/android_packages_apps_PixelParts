/*
 * Copyright (C) 2023 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.evolution.pixelparts.autohbm.AutoHbm;
import org.evolution.pixelparts.fastcharge.FastCharge;
import org.evolution.pixelparts.pixeltorch.*;
import org.evolution.pixelparts.saturation.Saturation;
import org.evolution.pixelparts.utils.ComponentUtils;
import org.evolution.pixelparts.utils.TorchUtils;

public class Startup extends BroadcastReceiver {

    private static final String TAG = Startup.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        // Auto hbm
        AutoHbm.toggleAutoHbmService(context);

        // Fast charge
        FastCharge.restoreFastChargeSetting(context);

        // Pixel torch
        PixelTorch.togglePixelTorchService(context);

        ComponentUtils.setComponentEnabled(
                context,
                PixelTorchActivity.class,
                TorchUtils.hasTorch(context)
        );

        // Saturation
        Saturation.restoreSaturationSetting(context);

    }
}
