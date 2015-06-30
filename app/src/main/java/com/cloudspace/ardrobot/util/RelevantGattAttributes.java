package com.cloudspace.ardrobot.util;

import java.util.HashMap;

public class RelevantGattAttributes {
    public static HashMap<String, String> attributes = new HashMap();
    public static final String IP_BROADCASTER_CHARACTERISTIC = "01010101-0101-0101-0166-616465524742";
    public static String IP_BROADCASTER = "01010101-0101-0101-0101-010101010101";

    static {
        attributes.put(IP_BROADCASTER, "IP Broadcaster");
    }
 
    public static String lookup(String uuid) {
        return attributes.get(uuid);
    }
}