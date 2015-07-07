package com.cloudspace.ardrobot.util;

import android.content.Context;

import com.koushikdutta.ion.Ion;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ken Kyger on 6/15/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class CylonApiBridge extends AbstractNodeMain {
    private static final long DELAY_IN_MILLIS = 0;
    private static CylonApiBridge instance = null;
    static HashMap<String, SubscriptionNode> apiTransformer;
    private static long last = -1;

    protected CylonApiBridge() {
        apiTransformer = new HashMap<>();
    }

    public static CylonApiBridge getInstance() {
        if (instance == null) {
            instance = new CylonApiBridge();
        }
        return instance;
    }

    public static void removeTranslation(String route) {
        apiTransformer.remove(route);
    }

    public static void addTranslation(final Context c, final Translation t) {
        if (!apiTransformer.containsKey(t.route)) {
            SubscriptionNode sub = new SubscriptionNode(new MessageListener() {
                @Override
                public void onNewMessage(final Object o) {
                    long now = System.currentTimeMillis();
                    if (last == -1 || now - last > DELAY_IN_MILLIS) {
                        last = now;
                        Ion.with(c).load(SettingsProvider.getIp(c) + t.route).setBodyParameter("data", t.tI.translate(o));
                    }
                }
            }, t.publisherNodeName, t.topicMessageType) {
                @Override
                public void onShutdownComplete(Node node) {

                }
            };
            apiTransformer.put(t.route, sub);
        }
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        for (Map.Entry<String, SubscriptionNode> entry : apiTransformer.entrySet()) {
            SubscriptionNode node = entry.getValue();
            org.ros.node.topic.Subscriber subscriber = connectedNode.newSubscriber(node.rosTopic, node.messageType);
            subscriber.addMessageListener(node.messageListener);
        }
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(getClass().getSimpleName());
    }


}
