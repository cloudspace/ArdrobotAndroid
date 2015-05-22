package com.cloudspace.ardrobot;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.cloudspace.ardrobot.util.BaseActivity;
import com.cloudspace.ardrobot.util.Constants;
import com.cloudspace.rosserial_android.ROSSerialADK;
import com.cloudspace.rosserial_java.NodeConnectionUtils;
import com.cloudspace.rosserial_java.TopicRegistrationListener;

import org.ros.address.InetAddressFactory;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.HashMap;

import rosserial_msgs.TopicInfo;

/**
 * Created by Ken Kyger on 5/21/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public abstract class ADKEnabledActivity extends BaseActivity {
    UsbAccessory mAccessory;
    ConnectedNode connectedNode;
    ROSSerialADK adk;
    AlertDialog errorDialog;
    HashMap<TopicInfo, Boolean> interestedTopics;

    private void handleNodesReadyInternally() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleNodesReady();
            }
        });
    }

    protected abstract void handleNodesReady();

    NodeConnectionUtils connectionUtils = new NodeConnectionUtils(new NodeConnectionUtils.OnNodeConnectedListener() {
        @Override
        public void onNodeConnected(ConnectedNode node) {
            connectedNode = node;
            attemptToSetAdk();
        }
    });

    private TopicRegistrationListener topicRegisteredListener = new TopicRegistrationListener() {
        @Override
        public void onNewTopic(final TopicInfo t) {
            interestedTopics.put(t, true);
            handleNodesReadyInternally();
        }
    };

    Handler errorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isActive()) {
                if (msg.getData() != null && msg.getData().get(Constants.EXTRA_ERROR) != null) {
                    int titleResId;
                    String message = (String) msg.getData().get(Constants.EXTRA_ERROR);
                    switch (msg.what) {
                        case ROSSerialADK.ERROR_ACCESSORY_CANT_CONNECT:
                            titleResId = R.string.unable_to_connect;
                            break;
                        case ROSSerialADK.ERROR_ACCESSORY_NOT_CONNECTED:
                            titleResId = R.string.unable_to_communicate;
                            break;
                        default:
                        case ROSSerialADK.ERROR_UNKNOWN:
                            titleResId = R.string.error_unknown;
                            break;
                    }
                    if (errorDialog == null) {
                        errorDialog = new AlertDialog.Builder(ADKEnabledActivity.this).setTitle(titleResId).setMessage(message).create();
                    } else {
                        errorDialog.setTitle(titleResId);
                        errorDialog.setMessage(message);
                    }
                    errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                            startActivity(new Intent(ADKEnabledActivity.this, AccessoryActivity.class));
                        }
                    });
                    errorDialog.show();
                }
            }
        }
    };

    private void attemptToSetAdk() {

        if (connectedNode != null && mAccessory != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        adk = new ROSSerialADK(errorHandler, ADKEnabledActivity.this, connectedNode, mAccessory);
                    } catch (Exception e) {
                        ROSSerialADK.sendError(errorHandler, ROSSerialADK.ERROR_ACCESSORY_CANT_CONNECT, e.getMessage());
                        return;
                    }
                    for (TopicInfo tI : adk.getPublications()) {
                        interestedTopics.put(tI, false);
                    }
                    for (TopicInfo tI : adk.getSubscriptions()) {
                        interestedTopics.put(tI, false);
                    }
                    adk.setOnSubscriptionCB(topicRegisteredListener);
                    adk.setOnSubscriptionCB(topicRegisteredListener);
                }
            });
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(Constants.EXTRA_ACCESSORY)) {
            mAccessory = getIntent().getParcelableExtra(Constants.EXTRA_ACCESSORY);
        } else {
            ROSSerialADK.sendError(errorHandler, ROSSerialADK.ERROR_ACCESSORY_NOT_CONNECTED, getResources().getString(R.string.no_accessory));
        }

        interestedTopics = new HashMap<>();
        attemptToSetAdk();
    }

    @Override
    public void init(NodeMainExecutor nodeMainExecutor) {
        nodeMainExecutor.execute(connectionUtils, NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri()));
        initNodes(nodeMainExecutor);
    }

    public abstract void initNodes(NodeMainExecutor nodeMainExecutor);
}
