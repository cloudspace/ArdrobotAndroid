package com.cloudspace.ardrobot.util;

import org.ros.internal.message.Message;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;

public abstract class SubscriptionNode<T extends Message> extends AbstractNodeMain {
    MessageListener messageListener;
    String rosTopic, messageType;
    T msgKind;

    /**
     * Create a new subscriber node.
     *
     * @param messageListener The callback to be notified when a new message arrives
     * @param rosTopic The name of the topic to subscribe to
     */
    public SubscriptionNode(MessageListener messageListener, String rosTopic) {
        this.messageListener = messageListener;
        this.rosTopic = rosTopic;
        this.messageType = msgKind.toRawMessage().getType();
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
