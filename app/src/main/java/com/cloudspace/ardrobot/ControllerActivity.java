package com.cloudspace.ardrobot;

import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

import com.cloudspace.ardrobot.util.BaseController;
import com.cloudspace.ardrobot.util.Constants;
import com.cloudspace.ardrobot.util.CustomVirtualJoystickView;
import com.cloudspace.ardrobot.util.CylonApiBridge;
import com.cloudspace.ardrobot.util.Vector3Translation;

import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import geometry_msgs.Twist;

/**
 * Created by cloudspace on 12/15/14.
 */
public class ControllerActivity extends BaseController {
    private static final String TAG = "CONTROLLER";
    public static final int PORT = 3000;

    private CustomVirtualJoystickView virtualJoystickView;
    CylonApiBridge apiBridge;

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        if (getMasterUri() != null && startedByUser) {
            super.init(nodeMainExecutor);

            NodeConfiguration controllerViewConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                    .setMasterUri(getMasterUri());
            controllerViewConfig.setNodeName(Constants.NODE_VIRTUAL_JOYSTICK);
            nodeMainExecutor
                    .execute(virtualJoystickView, controllerViewConfig);

            NodeConfiguration mapConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                    .setMasterUri(getMasterUri());
            nodeMainExecutor
                    .execute(apiBridge, mapConfig);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ViewFlipper) findViewById(R.id.flipper)).setDisplayedChild(1);
                    virtualJoystickView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        apiBridge = CylonApiBridge.getInstance();
        apiBridge.removeTranslation("/api/robots/ardrobot/commands/set_angle");
        apiBridge.removeTranslation("/api/robots/ardrobot/commands/set_speed");
        apiBridge = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        virtualJoystickView = (CustomVirtualJoystickView) findViewById(R.id.virtual_joystick);
        virtualJoystickView.setVisibility(View.VISIBLE);
        virtualJoystickView.setNodeName(Constants.NODE_VIRTUAL_JOYSTICK + "cmd_vel");

        apiBridge = CylonApiBridge.getInstance();
        apiBridge.addTranslation(this, new Vector3Translation("/api/robots/ardrobot/commands/set_angle",
                Twist._TYPE, Constants.NODE_VIRTUAL_JOYSTICK + "cmd_vel", new Vector3Translation.TranslateInterface() {
            @Override
            public String translate(org.ros.rosjava_geometry.Vector3[] messageValues) {
                return String.valueOf(messageValues[0].getX() * 100);
            }
        }));
        apiBridge.addTranslation(this, new Vector3Translation("/api/robots/ardrobot/commands/set_speed",
                Twist._TYPE, Constants.NODE_VIRTUAL_JOYSTICK + "cmd_vel", new Vector3Translation.TranslateInterface() {
            @Override
            public String translate(org.ros.rosjava_geometry.Vector3[] messageValues) {
                return String.valueOf(messageValues[1].getZ() * 100);
            }
        }));
    }
}