package com.cloudspace.ardrobot;

import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import java.lang.String;

import std_msgs.*;

public class Listener extends AbstractNodeMain {
    private static boolean status;
    MessageListener<std_msgs.String> messageListener;
    String rosTopic;

    public Listener(MessageListener<std_msgs.String> messageListener, String rosTopic) {
        this.messageListener = messageListener;
        this.rosTopic = rosTopic;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ardrobot_control/listener");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        final Log log = connectedNode.getLog();
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(rosTopic, std_msgs.String._TYPE);
        subscriber.addMessageListener(messageListener);
    }
}
