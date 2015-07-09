package com.cloudspace.ardrobot.util;

import java.util.HashMap;

public class RelevantGattAttributes {
    public static final String NOTIFICATION_ENABLE_ATTRIBUTE = "00002902-0000-1000-8000-00805f9b34fb";
    public static HashMap<String, String> attributes = new HashMap();
    public static final String IP_BROADCASTER_CHARACTERISTIC = "01010101-0101-0101-0166-616465524742";
    public static final String IP_BROADCASTER = "01010101-0101-0101-0101-010101010101";

    static {
        attributes.put(IP_BROADCASTER, "IP Broadcaster");
    }
 
    public static String lookup(String uuid) {
        return attributes.get(uuid);
    }
}