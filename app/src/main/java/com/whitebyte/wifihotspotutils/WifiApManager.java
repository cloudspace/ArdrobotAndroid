/*
 * Copyright 2013 WhiteByte (Nick Russler, Ahmet Yueksektepe).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.whitebyte.wifihotspotutils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;

public class WifiApManager {
	private final WifiManager mWifiManager;
	private Context context;

	public WifiApManager(Context context) {
		this.context = context;
		mWifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
	}

	/**
	 * Start AccessPoint mode with the specified
	 * configuration. If the radio is already running in
	 * AP mode, update the new configuration
	 * Note that starting in access point mode disables station
	 * mode operation
	 * @return {@code true} if the operation succeeds, {@code false} otherwise
	 */
	public boolean setWifiApEnabled(boolean enabled) {
		try {
			if (enabled) { // disable WiFi in any case
				mWifiManager.setWifiEnabled(false);
			}

			Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			return (Boolean) method.invoke(mWifiManager, null, enabled);
		} catch (Exception e) {
			Log.e(this.getClass().toString(), "", e);
			return false;
		}
	}

	/**
	 * Gets the Wi-Fi AP Configuration.
	 * @return AP details in {@link WifiConfiguration}
	 */
	public WifiConfiguration getWifiApConfiguration() {
		try {
			Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
			return (WifiConfiguration) method.invoke(mWifiManager);
		} catch (Exception e) {
			Log.e(this.getClass().toString(), "", e);
			return null;
		}
	}

	/**
	 * Sets the Wi-Fi AP Configuration.
	 * @return {@code true} if the operation succeeded, {@code false} otherwise
	 */
	public boolean setWifiApConfiguration(WifiConfiguration wifiConfig) {
		try {
			Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
			return (Boolean) method.invoke(mWifiManager, wifiConfig);
		} catch (Exception e) {
			Log.e(this.getClass().toString(), "", e);
			return false;
		}
	}
}
