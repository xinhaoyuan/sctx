package org.sctx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
	
	HashMap<String, HashSet<WifiRule>> ssidToRules;
	HashSet<WifiRule> rules;
	
	WifiContext(SmartContext ctx) {
		this.ctx = ctx;
	}
	
	void onCreate() {
		wifiManager = (WifiManager)ctx.getSystemService(wifi_service);
		wifiListener = new WifiListener();
		wifiLastScanResult = null;
		ssidToRules = new HashMap<String, HashSet<WifiRule>>();
		rules = new HashSet<WifiRule>();
	}
	
	void bind() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(wifi_state_changed_action);
		intentFilter.addAction(wifi_rssi_changed_action);
		ctx.registerReceiver(wifiListener, intentFilter);
	}
	
	void addRule(WifiRule rule) {
		if (rules.contains(rule)) return;
		
		rule.lastRefCount = rule.refCount = 0;
		
		if (!ssidToRules.containsKey(rule.ssid)) {
			ssidToRules.put(rule.ssid, new HashSet<WifiRule>());
		}
		ssidToRules.get(rule.ssid).add(rule);
		rules.add(rule);
	}
	
	void removeRule(WifiRule rule) {
		if (!rules.contains(rule)) return;
		HashSet<WifiRule> r = ssidToRules.get(rule.ssid);
		r.remove(rule);
		if (r.isEmpty()) ssidToRules.remove(rule.ssid);
		rules.remove(rule);
	}
	
	void unbind() {
		ctx.unregisterReceiver(wifiListener);
	}
	
	void onDestroy() {
	}
	
	void setWifiScanResult(List<ScanResult> result) {
		Iterator<WifiRule> it = rules.iterator();
		while (it.hasNext()) {
			WifiRule rule = it.next();
			rule.lastRefCount = rule.refCount;
			rule.refCount = 0;
		}
		
		for (ScanResult item : result) {
			if (!ssidToRules.containsKey(item.SSID)) continue;
			it = ssidToRules.get(item.SSID).iterator();
			while (it.hasNext())
			{
				WifiRule rule = it.next();
				if (rule.match(item.SSID, item.BSSID, item.level)) ++ rule.refCount;
			}
		}
		
		it = rules.iterator();
		while (it.hasNext()) {
			WifiRule rule = it.next();
			if (rule.lastRefCount == 0 && rule.refCount > 0)
				ctx.getContext(rule.result_context);
			else if (rule.lastRefCount > 0 && rule.refCount == 0)
				ctx.putContext(rule.result_context);
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
				// Set result to empty
				setWifiScanResult(new LinkedList<ScanResult>());
			} else if (action == WifiContext.wifi_rssi_changed_action && wifiEnabled) {
				setWifiScanResult(wifiManager.getScanResults());
			}
		}
	}
}

class WifiRule {
	static final int LIST_TYPE_WHITE = 0;
	static final int LIST_TYPE_BLACK = 1;
	
	String ssid;
	String result_context;
	int level_threshold;
	int list_type = LIST_TYPE_BLACK;
	HashSet<String> essid_list;
	int lastRefCount = 0;
	int refCount = 0;
	
	boolean match(String ssid, String essid, int level) {
		if (!ssid.equals(this.ssid)) return false;
		if (list_type == LIST_TYPE_WHITE) {
			return (essid_list != null && essid_list.contains(essid)) && level > level_threshold;
		} else {
			return !(essid_list != null && essid_list.contains(essid)) && level > level_threshold;
		}
	}
}
