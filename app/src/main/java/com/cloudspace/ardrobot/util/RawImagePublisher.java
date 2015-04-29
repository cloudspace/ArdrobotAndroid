//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.cloudspace.ardrobot.util;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;

import com.google.common.base.Preconditions;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;

class RawImagePublisher implements CustomRawImageListener {
    public static final String CAMERA_NODE_BASE = "camera";
    public static final String IMAGE_COMPRESSED_NODE = "image/compressed";
    public static final String CAMERA_INFO_NODE = "camera_info";
    public static final String CAMERA_FRAME_ID = "camera";
    public static final String IMAGE_FORMAT = "jpeg";
    private final ConnectedNode connectedNode;
    private final Publisher<CompressedImage> imagePublisher;
    private final Publisher<CameraInfo> cameraInfoPublisher;
    private byte[] rawImageBuffer;
    private Size rawImageSize;
    private YuvImage yuvImage;
    private Rect rect;
    private ChannelBufferOutputStream stream;
    private int quality = -1;

    public RawImagePublisher(ConnectedNode connectedNode, int quality) {
        this.connectedNode = connectedNode;
        NameResolver resolver = connectedNode.getResolver().newChild(CAMERA_NODE_BASE);
        this.imagePublisher = connectedNode.newPublisher(resolver.resolve(IMAGE_COMPRESSED_NODE), CompressedImage._TYPE);
        this.cameraInfoPublisher = connectedNode.newPublisher(resolver.resolve(CAMERA_INFO_NODE), CameraInfo._TYPE);
        this.stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        if (quality < 0 || quality > 100) {
            quality = 20;
        } else {
            this.quality = quality;
        }
    }

    public void onNewRawImage(byte[] data, Size size) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(size);
        if(data != this.rawImageBuffer || !size.equals(this.rawImageSize)) {
            this.rawImageBuffer = data;
            this.rawImageSize = size;
            this.yuvImage = new YuvImage(this.rawImageBuffer, 17, size.width, size.height, (int[])null);
            this.rect = new Rect(0, 0, size.width, size.height);
        }

        Time currentTime = this.connectedNode.getCurrentTime();
        CompressedImage image = (CompressedImage)this.imagePublisher.newMessage();
        image.setFormat(IMAGE_FORMAT);
        image.getHeader().setStamp(currentTime);
        image.getHeader().setFrameId(CAMERA_FRAME_ID);
        Preconditions.checkState(this.yuvImage.compressToJpeg(this.rect, quality, this.stream));
        image.setData(this.stream.buffer().copy());
        this.stream.buffer().clear();
        this.imagePublisher.publish(image);
        CameraInfo cameraInfo = (CameraInfo)this.cameraInfoPublisher.newMessage();
        cameraInfo.getHeader().setStamp(currentTime);
        cameraInfo.getHeader().setFrameId(CAMERA_FRAME_ID);
        cameraInfo.setWidth(size.width);
        cameraInfo.setHeight(size.height);
        this.cameraInfoPublisher.publish(cameraInfo);
    }
}
