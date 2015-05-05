package com.cloudspace.ardrobot.util;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;

public abstract class SubscriptionNode extends AbstractNodeMain {
    MessageListener messageListener;
    String rosTopic, messageType;

    /**
     * Create a new subscriber node.
     *
     * @param messageListener The callback to be notified when a new message arrives
     * @param rosTopic The name of the topic to subscribe to
     * @param messageType    The ._TYPE of the message
     *
     */
    public SubscriptionNode(MessageListener messageListener, String rosTopic, String messageType) {
        this.messageListener = messageListener;
        this.rosTopic = rosTopic;
        this.messageType = messageType;
    }

    @Override
    public abstract void onShutdownComplete(Node node);

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.newAnonymous();
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        org.ros.node.topic.Subscriber subscriber = connectedNode.newSubscriber(rosTopic, messageType);
        subscriber.addMessageListener(messageListener);
    }
}
