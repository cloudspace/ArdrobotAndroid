package com.cloudspace.ardrobot.util;

import android.os.Environment;

/**
 * Created by Ken Kyger on 5/4/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class Constants {

    public static final String NODE_PREFIX_IMU = "imu/";
    public static final String NODE_PREFIX_CONTROLLER = "controller/";
    public static final String NODE_PREFIX_ROBOT = "robot/";
    public static final String NODE_SUFFIX_AUDIO_WATCHER = "audio_watcher/";

    public static final String NODE_IMU_HEAD = NODE_PREFIX_IMU + "head/";
    public static final String NODE_IMU_CONTROLLER = NODE_PREFIX_IMU + "controller/";

    public static final String NODE_VIRTUAL_JOYSTICK = NODE_PREFIX_CONTROLLER + "joystick/";
    public static final String NODE_SENSOR_KILLSWITCH = "sensor_killswitch/";
    public static final String NODE_AUDIO_STATE = "audio_state/";
    private static final String NODE_PREFIX_CAMERA = "camera/";
    public static final String NODE_IMAGE_COMPRESSED = NODE_PREFIX_CAMERA + "image/compressed/";

    public static final String PREF_IP = "ip";

    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_ACCESSORY = "accessory";
    public static final String EXTRA_RESULT = "result";

    public static final String PATH_OVPN_APK = Environment.getExternalStorageDirectory() + "/open_vpn.apk";
    public static final String PATH_OVPN_CLIENT_CONFIG = Environment.getExternalStorageDirectory() + "/client.ovpn";

    public static final String VPN_SERVICE_PACKAGE = "de.blinkt.openvpn";
    public static final String VPN_CONNECTED_SUCCESS = "CONNECTED|SUCCESS";
    public static final String VPN_NOPROCESS = "NOPROCESS";

    public static final String AUDIO_FROM_CONTROLLER = "audio_from_controller";
    public static final String AUDIO_FROM_ROBOT = "audio_from_robot";
    public static final String AUDIO_BOTH = "audio_both";

    public static final String ACTION_START_NODE_RUNNER_SERVICE = "org.ros.android.ACTION_START_NODE_RUNNER_SERVICE";

    public static final String APK_DATA_TYPE = "application/vnd.android.package-archive";

}
