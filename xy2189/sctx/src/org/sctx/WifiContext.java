package org.sctx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

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
	
	class WifiScanPusher implements Runnable {
		boolean active;
		
		public void run() {
			if (!active) return;
			if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
				Util.log("Requested to refresh wifi");
				wifiManager.startScan();
			}
			Util.runInSCThreadDelayed(this, 5000);
		}
	}
	
	WifiScanPusher pusher;
	
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
		
		pusher = new WifiScanPusher();
		pusher.active = true;
		Util.runInSCThread(pusher);
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
	
	void addRulesFromReader(Reader r) {
		BufferedReader in = new BufferedReader(r);
		while (true) {
			String line;
			try {
				line = in.readLine();
			} catch(Exception x) { line = null; }
			if (line == null) break;
			
			Scanner s = new Scanner(line);
			s.useDelimiter(",");
			
			try {
				String context = "Wifi@" + Util.decodeString(s.next());
				String ssid = Util.decodeString(s.next());
				int threshold = s.nextInt();
				String type = s.next();
				int list_item_count = s.nextInt();
				HashSet<String> list = new HashSet<String>();
				for (int i = 0; i < list_item_count; ++ i)
				{
					list.add(s.next());
				}
				// hasNext indicate format error
				if (s.hasNext()) continue;
				
				WifiRule rule = new WifiRule();
				rule.result_context = context;
				rule.ssid = ssid;
				rule.level_threshold = threshold;
				rule.list_type = type.equalsIgnoreCase("black") ? WifiRule.LIST_TYPE_BLACK : WifiRule.LIST_TYPE_WHITE;
				rule.essid_list = list;
				addRule(rule);
			} catch (Exception x) {
				continue;
			}
		}
	}
	
	void unbind() {
		pusher.active = false;
		pusher = null;
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
			Log.i("wifiResult", item.SSID);
			
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
				ctx.getNativeContext(rule.result_context);
			else if (rule.lastRefCount > 0 && rule.refCount == 0)
				ctx.putNativeContext(rule.result_context);
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
	
	void removeRules(String symbolName, String ssid) {
		Util.log("remove rules in conf " + symbolName + "," + ssid);
		
		Iterator<WifiRule> rit = rules.iterator();
		ArrayList<WifiRule> delList = new ArrayList<WifiRule>();
		
		while (rit.hasNext()) {
			WifiRule r = rit.next();
			if ((symbolName == null || symbolName.equals(r.result_context)) &&
				(ssid == null || ssid.equals(r.ssid))) {
				delList.add(r);
			}
		}
		
		rit = delList.iterator();
		while (rit.hasNext()) {
			WifiRule r = rit.next();
			removeRule(r);
		}
		
		Util.log("" + delList.size() + " rules removed");
	}
	
	void saveRulesToLocal() {
		BufferedWriter out;
		try {
			OutputStream o = ctx.openFileOutput("wifi_context.txt", 0);
			out = new BufferedWriter(new OutputStreamWriter(o));
		} catch (Exception x) {
			x.printStackTrace();
			return;
		}
		
		HashMap<String, HashSet<String>> conf = new HashMap<String, HashSet<String>>();
		Iterator<WifiRule> rit = rules.iterator();
		while (rit.hasNext())
		{
			WifiRule r = rit.next();
			if (!conf.containsKey(r.result_context))
				conf.put(r.result_context, new HashSet<String>());
			conf.get(r.result_context).add(r.ssid);
		}
		
		Iterator<String> sit = conf.keySet().iterator();
		while (sit.hasNext()) {
			String symbol = sit.next();
			Iterator<String> idit = conf.get(symbol).iterator();
			symbol = symbol.substring("Wifi@".length());
			while (idit.hasNext())
			{
				String ssid = idit.next();
				try {
					out.write(Util.encodeString(symbol) + "," + Util.encodeString(ssid) + ",-200,BLACK,0");
					out.newLine();
				} catch (Exception x) {
					x.printStackTrace();
					return;
				}
			}
		}
		
		try { out.close(); } catch (Exception x) {
			x.printStackTrace();
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
