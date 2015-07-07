package com.cloudspace.ardrobot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ken Kyger on 7/7/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class SettingsProvider {

    public static void setIp(byte[] ipArray, final Context ctx) {
        final String ip = new String(ipArray);
        if (ip.equals(PreferenceManager.getDefaultSharedPreferences(ctx).getString("ip", "")) || ip.equals("127.0.0.1"))
            return;
        Pattern pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        Matcher matcher = pattern.matcher(ip);
        if (!matcher.matches()) return;

        Log.d("ARDROBOT", "http://" + ip + ":3000");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, "http://" + ip + ":3000", Toast.LENGTH_LONG).show();
            }
        });


        if (ip.equals(PreferenceManager.getDefaultSharedPreferences(ctx).getString("ip", "")))
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString("ip", ip).apply();
    }

    public static String getIp(Context ctx) {
        return "http://" + PreferenceManager.getDefaultSharedPreferences(ctx).getString("ip", "") + ":3000";
    }

    public static void setAddress(String address, final Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString("address", address).apply();
    }

    public static String getEdisonAddress(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString("address", "");
    }
}
