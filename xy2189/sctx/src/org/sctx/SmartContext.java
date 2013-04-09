package org.sctx;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

public class SmartContext extends Service {
	
	Handler handler;
	Messenger msger;
	
	static final String wifi_service = Context.WIFI_SERVICE;
	static final String wifi_state_changed_action = WifiManager.NETWORK_STATE_CHANGED_ACTION;
	static final String wifi_rssi_changed_action = WifiManager.RSSI_CHANGED_ACTION;
	WifiManager wifiManager;
	WifiListener wifiListener;
	boolean wifiEnabled;
	List<ScanResult> wifiLastScanResult;
	
	@Override
	public void onCreate() {
		handler = new Handler();
		msger = new Messenger(handler);
		
		wifiManager = (WifiManager)getSystemService(wifi_service);
		wifiListener = new WifiListener();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(wifi_state_changed_action);
		intentFilter.addAction(wifi_rssi_changed_action);
		registerReceiver(wifiListener, intentFilter);
		
		Util.log("SmartContext service created");
	}
	
	@Override
	public void onDestroy() {
		Util.log("Destroying the SmartContext service");
		unregisterReceiver(wifiListener);
	}
	
	@SuppressLint("HandlerLeak")
	class RequestHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Util.log("Got request what = " + msg.what);
		}
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return msger.getBinder();
    }
	
	void setWifiScanResult(List<ScanResult> result) {
		for (ScanResult item : result) {
			// Util.log("Got network: " + item.SSID + " with BSSID " + item.BSSID + " and strength " + item.level);
		}
		wifiLastScanResult = result;
	}
	
	class WifiListener extends BroadcastReceiver {
		public WifiListener() {
			wifiEnabled = wifiManager.getWifiState() == wifiManager.WIFI_STATE_ENABLED;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == SmartContext.wifi_state_changed_action) {
				wifiEnabled = wifiManager.getWifiState() == wifiManager.WIFI_STATE_ENABLED;
			} else if (action == SmartContext.wifi_rssi_changed_action && wifiEnabled) {
				setWifiScanResult(wifiManager.getScanResults());
			}
		}
	}
}
