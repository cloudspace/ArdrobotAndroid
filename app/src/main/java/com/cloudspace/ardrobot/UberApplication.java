package com.cloudspace.ardrobot;

import android.app.Application;
import android.content.Intent;

import com.cloudspace.ardrobot.util.BleScanService;

/**
 * Created by Ken Kyger on 7/23/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class UberApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, BleScanService.class));
    }
}
