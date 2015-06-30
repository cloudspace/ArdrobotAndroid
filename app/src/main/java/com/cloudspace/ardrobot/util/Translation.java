package com.cloudspace.ardrobot.util;

public class Translation {
    String route;
    String topicMessageType;
    String publisherNodeName;
    TranslateInterface tI;

    public Translation(String route, String topicMessageType, String publisherNodeName, TranslateInterface tI) {
        this.route = route;
        this.topicMessageType = topicMessageType;
        this.publisherNodeName = publisherNodeName;
        this.tI = tI;
    }

    public interface TranslateInterface<T> {
        String translate(T o);
    }
}