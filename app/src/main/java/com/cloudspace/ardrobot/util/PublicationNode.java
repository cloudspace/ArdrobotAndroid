package com.cloudspace.ardrobot.util;

import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

public abstract class PublicationNode<T extends Message> extends AbstractNodeMain {
    Publisher messagePublisher;
    String rosTopic, messageType;
    T msgKind;

    /**
     * Create a new subscriber node.
     *
     * @param rosTopic    The name of the topic to subscribe to
     */
    public PublicationNode(String rosTopic) {
        this.rosTopic = rosTopic;
        messageType = msgKind.toRawMessage().getType();
    }

    @Override
    public abstract void onShutdownComplete(Node node);

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.newAnonymous();
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        messagePublisher =
                connectedNode.newPublisher(rosTopic, messageType);
    }

    public boolean isConnected() {
        return messagePublisher != null;
    }

    /**
     *
     * @param message
     * @throws IllegalArgumentException thrown when message and publisher types do not match
     */
    public void publish(Message message) {
        if (!message.toRawMessage().getType().equals(messageType)) {
            throw new IllegalArgumentException("Message of the type: " +
                    message.toRawMessage().getType() + " does not match statePublisher type: " +
                    messageType);
        } else {
            messagePublisher.publish(message);
        }
    }

    public Object newMessage() {
        return messagePublisher.newMessage();
    }
}
