package com.cloudspace.ardrobot;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import std_msgs.String;

public class Talker extends AbstractNodeMain {
   java.lang.String content, channel;
    Publisher<String> publisher;
    
    public Talker(java.lang.String channel) {
        this.channel = channel;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("ardrobot_control/" + channel);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher(channel, String._TYPE);       
    }
    
    public void publish(java.lang.String content) { 
        this.content = content;
        String message = publisher.newMessage();
        message.setData(content);
        publisher.publish(message);
    }
}
