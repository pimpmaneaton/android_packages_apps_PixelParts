/*
 * Copyright (C) 2023 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts.pixeltorch;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import org.evolution.pixelparts.Constants;

public class PixelTorchService extends Service implements SensorEventListener {

    private CameraManager mCameraManager;
    private CameraManager.TorchCallback mTorchCallback;

    private boolean mTorchEnabled = false;
    private PowerManager mPowerManager;
    private WakeLock mWakeLock;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long mLastShakeTime = 0;
    private int mTorchStrength;
    private int mShakeThreshold;
    private int mShakeSensitivity;

    @Override
    public void onCreate() {
        super.onCreate();

        mCameraManager = getSystemService(CameraManager.class);
        registerTorchCallback();

        mPowerManager = getSystemService(PowerManager.class);
        mWakeLock = mPowerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "PixelTorchWakeLock");
        mWakeLock.acquire();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mTorchStrength = prefs.getInt(Constants.KEY_PIXEL_TORCH_STRENGTH, 45);
        mShakeThreshold = prefs.getInt(Constants.KEY_SHAKE_THRESHOLD, 1024);
        mShakeSensitivity = prefs.getInt(Constants.KEY_SHAKE_SENSITIVITY, 400);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCameraManager.unregisterTorchCallback(mTorchCallback);

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void detectShake(float[] acceleration) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastShakeTime > mShakeThreshold) {
            float totalAcceleration = (float) Math.sqrt(
                acceleration[0] * acceleration[0] +
                acceleration[1] * acceleration[1] +
                acceleration[2] * acceleration[2]
            );

            if (totalAcceleration > mShakeSensitivity) {
                mLastShakeTime = currentTime;
                toggleTorch();
            }
        }
    }

    private void toggleTorch() {
        try {
            String outCameraId = mCameraManager.getCameraIdList()[0];
            if (mTorchEnabled) {
                turnOffTorch(outCameraId);
            } else {
                turnOnTorch(outCameraId);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void turnOffTorch(String outCameraId) {
        try {
            mCameraManager.setTorchMode(outCameraId, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnOnTorch(String outCameraId) {
        try {
            if (mTorchStrength != 0) {
                mCameraManager.turnOnTorchWithStrengthLevel(outCameraId, mTorchStrength);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerTorchCallback() {
        mTorchCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                mTorchEnabled = enabled;
            }
        };
        mCameraManager.registerTorchCallback(mTorchCallback, null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
