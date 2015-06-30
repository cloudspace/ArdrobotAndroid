package com.cloudspace.ardrobot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.koushikdutta.ion.Ion;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        Ion.with(c).load(getIp(c) + t.route).setBodyParameter("data", t.tI.translate(o));
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

    public static void setIp(byte[] ipArray, final Context ctx) {
        final String ip = new String(ipArray);
        if (ip.equals(PreferenceManager.getDefaultSharedPreferences(ctx).getString("ip", "")) || ip.equals("127.0.0.1")) return;
        Pattern pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        Matcher matcher = pattern.matcher(ip);
        if (!matcher.matches()) return;

        Log.d("ARDROBOT", "http://" + ip + ":3000");
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, "http://" + ip + ":3000", Toast.LENGTH_LONG).show();
            }
        });


        if (ip.equals(PreferenceManager.getDefaultSharedPreferences(ctx).getString("ip", ""))) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString("ip", ip).apply();
    }

    public static String getIp(Context ctx) {
        return "http://" + PreferenceManager.getDefaultSharedPreferences(ctx).getString("ip", "") + ":3000";
    }
}
