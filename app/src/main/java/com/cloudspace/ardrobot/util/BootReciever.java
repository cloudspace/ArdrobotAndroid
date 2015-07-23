package com.cloudspace.ardrobot.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Ken Kyger on 6/26/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class BootReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, BleScanService.class));
    }
}
