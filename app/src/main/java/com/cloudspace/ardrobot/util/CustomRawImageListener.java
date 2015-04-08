package com.cloudspace.ardrobot.util;

import android.hardware.Camera;

interface CustomRawImageListener {
    void onNewRawImage(byte[] var1, Camera.Size var2);
}