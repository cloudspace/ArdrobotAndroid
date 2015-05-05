package com.cloudspace.ardrobot.util.custom_ros;

import android.hardware.Camera;

public interface CustomRawImageListener {
    void onNewRawImage(byte[] var1, Camera.Size var2);
}