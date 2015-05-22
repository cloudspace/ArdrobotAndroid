package com.cloudspace.ardrobot;

import android.os.Bundle;

import com.cloudspace.ardrobot.util.BaseActivity;
import com.cloudspace.rosjava_video.TwoWayVideoView;
import com.cloudspace.rosjava_video.VideoConfig;

import org.ros.address.InetAddressFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;


public class VideoConferenceTest extends BaseActivity {
    TwoWayVideoView twoWayVideo;
    public static final boolean IS_ROBOT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_conference_test);
        twoWayVideo = (TwoWayVideoView) findViewById(R.id.two_way_view);
        twoWayVideo.setConfig(getConfig());
    }

    @Override
    public void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration config = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostName())
                .setMasterUri(getMasterUri());

        nodeMainExecutor.execute(twoWayVideo, config);
    }

    public VideoConfig getConfig() {
        if (IS_ROBOT) {
            return new VideoConfig("video/to_robot", "video/to_controller").withOutGoingQuality(100);
        } else {
            return new VideoConfig("video/to_controller", "video/to_robot").withOutGoingQuality(100);
        }
    }
}
