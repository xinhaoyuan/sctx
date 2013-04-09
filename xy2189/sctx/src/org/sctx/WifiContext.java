package org.sctx;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

class WifiContext {
	static final String wifi_service = Context.WIFI_SERVICE;
	static final String wifi_state_changed_action = WifiManager.NETWORK_STATE_CHANGED_ACTION;
	static final String wifi_rssi_changed_action = WifiManager.RSSI_CHANGED_ACTION;
	
	SmartContext ctx;
	
	WifiManager wifiManager;
	WifiListener wifiListener;
	boolean wifiEnabled;
	List<ScanResult> wifiLastScanResult;
	
	WifiContext(SmartContext ctx) {
		this.ctx = ctx;
	}
	
	void onCreate() {
		wifiManager = (WifiManager)ctx.getSystemService(wifi_service);
		wifiListener = new WifiListener();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(wifi_state_changed_action);
		intentFilter.addAction(wifi_rssi_changed_action);
		ctx.registerReceiver(wifiListener, intentFilter);
	}
	
	void onDestroy() {
		ctx.unregisterReceiver(wifiListener);
	}
	
	void setWifiScanResult(List<ScanResult> result) {
		for (ScanResult item : result) {
			Util.log("Got network: " + item.SSID + " with BSSID " + item.BSSID + " and strength " + item.level);
		}
		wifiLastScanResult = result;
	}
	
	class WifiListener extends BroadcastReceiver {
		public WifiListener() {
			wifiEnabled = wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == WifiContext.wifi_state_changed_action) {
				wifiEnabled = wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
			} else if (action == WifiContext.wifi_rssi_changed_action && wifiEnabled) {
				setWifiScanResult(wifiManager.getScanResults());
			}
		}
	}
}
