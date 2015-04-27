package com.cloudspace.ardrobot.util;

import android.content.Intent;

import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

/**
 * Created by FutureHax on 4/7/15.
 */
public abstract class BaseActivity extends RosActivity {

    boolean isActive = false;

    public boolean isActive() {
        return isActive;
    }

    public BaseActivity(String notificationTicker, String notificationTitle) {
        super(notificationTicker, notificationTitle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, NodeMainExecutorService.class);
        intent.setAction("org.ros.android.ACTION_START_NODE_RUNNER_SERVICE");
        stopService(intent);
    }

    public BaseActivity(String notificationTicker, String notificationTitle, URI customMasterUri) {
        super(notificationTicker, notificationTitle, customMasterUri);
    }

    public BaseActivity() {
        super("Ardrobot is running.", "Ardrobot", URI.create("http://10.100.4.65:11311"));
    }

    @Override
    protected abstract void init(NodeMainExecutor nodeMainExecutor);

    @Override
    protected void onPause() {
        super.onPause();
        isActive = false;
    }

    @Override
    protected void onResume() {

        super.onResume();
        isActive = true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
