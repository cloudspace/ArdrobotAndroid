package com.cloudspace.ardrobot;

import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

import com.cloudspace.ardrobot.util.BaseController;
import com.cloudspace.ardrobot.util.Constants;

import org.ros.address.InetAddressFactory;
import org.ros.android.view.VirtualJoystickView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

/**
 * Created by cloudspace on 12/15/14.
 */
public class ControllerActivity extends BaseController {
    private static final String TAG = "CONTROLLER";


    private VirtualJoystickView virtualJoystickView;

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        if (getMasterUri() != null && startedByUser) {
            super.init(nodeMainExecutor);

            NodeConfiguration controllerViewConfig = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                    .setMasterUri(getMasterUri());
            controllerViewConfig.setNodeName(Constants.NODE_VIRTUAL_JOYSTICK);

            nodeMainExecutor
                    .execute(virtualJoystickView, controllerViewConfig);

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
        virtualJoystickView.setVisibility(View.VISIBLE);
    }
}