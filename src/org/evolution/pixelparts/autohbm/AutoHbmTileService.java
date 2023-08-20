/*
 * Copyright (C) 2023 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts.autohbm;

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.evolution.pixelparts.Constants;
import org.evolution.pixelparts.R;
import org.evolution.pixelparts.utils.FileUtils;

public class AutoHbmTileService extends TileService {

    private void updateTile(boolean enabled) {
        final Tile tile = getQsTile();
        if (FileUtils.isFileWritable(Constants.NODE_HBM)) {
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            String subtitle = enabled ? getString(R.string.tile_on) : getString(R.string.tile_off);
            tile.setSubtitle(subtitle);
            tile.updateTile();
        } else {
            tile.setState(Tile.STATE_UNAVAILABLE);
        }
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateTile(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean enabled = !(sharedPrefs.getBoolean(Constants.KEY_AUTO_HBM, false));
        sharedPrefs.edit().putBoolean(Constants.KEY_AUTO_HBM, enabled).commit();
        AutoHbm.toggleAutoHbmService(this);
        updateTile(enabled);
    }
}
