package com.cloudspace.ardrobot.util.custom_ros;
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;
import org.ros.android.MessageCallable;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

public abstract class CustomRosImageView<T> extends ImageView implements NodeMain {
    private String topicName;
    private String messageType;
    private MessageCallable<Bitmap, T> callable;

    public CustomRosImageView(Context context) {
        super(context);
    }

    public CustomRosImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRosImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setMessageToBitmapCallable(MessageCallable<Bitmap, T> callable) {
        this.callable = callable;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("ros_image_view");
    }

    public void onStart(ConnectedNode connectedNode) {
        Subscriber subscriber = connectedNode.newSubscriber(this.topicName, this.messageType);
        subscriber.addMessageListener(new MessageListener<T>() {
            public void onNewMessage(final T message) {
                CustomRosImageView.this.post(new Runnable() {
                    public void run() {
                        Bitmap b = callable.call(message);
                        setImageBitmap(b);
                    }
                });
                postInvalidate();
            }
        });
    }




    public void onError(Node node, Throwable throwable) {
    }
}
