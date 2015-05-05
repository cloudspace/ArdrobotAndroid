//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.cloudspace.ardrobot.util.custom_ros;

import android.content.Context;
import android.util.AttributeSet;

import com.cloudspace.ardrobot.util.Constants;
import com.cloudspace.ardrobot.util.RawImagePublisher;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;

public class CustomRosCameraPreviewView extends CustomCameraPreviewView implements NodeMain {
    public int width = -1;
    public int height = -1;
    private int quality = 20;

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        width = xNew;
        height = yNew;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public CustomRosCameraPreviewView(Context context) {
        super(context);
    }

    public CustomRosCameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRosCameraPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of(Constants.NODE_CAMERA_PREVIEW);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.setRawImageListener(new RawImagePublisher(connectedNode, quality));
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    public void onError(Node node, Throwable throwable) {

    }
}
