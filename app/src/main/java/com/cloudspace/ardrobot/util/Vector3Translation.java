package com.cloudspace.ardrobot.util;

import org.ros.rosjava_geometry.Vector3;

public class Vector3Translation extends Translation {

    public Vector3Translation(String route, String topicMessageType, String publisherNodeName, TranslateInterface tI) {
        super(route, topicMessageType, publisherNodeName, tI);
    }

    public interface TranslateInterface extends Translation.TranslateInterface<Vector3[]>{
        String translate(Vector3[] messageValues);
    }
}