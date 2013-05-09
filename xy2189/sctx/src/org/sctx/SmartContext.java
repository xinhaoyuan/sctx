package org.sctx;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SmartContext extends Service {
	
	Handler handler;
	Messenger msger;
	
	WifiContext wifi;
	MotionContext motion;
	SoundContext sound;
	
	DemoLuaRuntime lua;
	
	HashMap<String, Integer> nativeContextRef;
	
	ContextInferenceEngine eng;
	
	void getNativeContext(String name) {
		if (nativeContextRef.containsKey(name)) {
			nativeContextRef.put(name, nativeContextRef.get(name) + 1);
		}
		else {
			nativeContextRef.put(name, 1);
			enterContext(name);
		}
	}
	
	void putNativeContext(String name) {
		if (nativeContextRef.containsKey(name)) {
			int value = nativeContextRef.get(name);
			if (value == 1)
			{
				nativeContextRef.remove(name);
				leaveContext(name);
			} else {
				nativeContextRef.put(name, value - 1);
			}
		}
	}
	
	void enterContext(String name) {
		ArrayList<String> updateList = new ArrayList<String>();
		eng.updateSystemSymbol(name, true, updateList);
		Iterator<String> it = updateList.iterator();
		while (it.hasNext()) {
			String symbol = it.next();
			notifyContext(symbol);
		}
	}
	
	void leaveContext(String name) {
		ArrayList<String> updateList = new ArrayList<String>();
		eng.updateSystemSymbol(name, false, updateList);
		Iterator<String> it = updateList.iterator();
		while (it.hasNext()) {
			String symbol = it.next();
			notifyContext(symbol);
		}
	}
	
	void notifyContext(final String name) {
		final boolean v = eng.symbols.get(name);
		Util.runInUIThread(new Runnable() {
			@Override
			public void run() {
				try {
					EntryActivity a = Singletons.ea.get();
					HashMap<String, TextView> map = a.contextViews;
					LinearLayout c = a.contextViewContainer;
					
					if (v) {
						TextView tag = new TextView(a);
						tag.setText(name);
						map.put(name, tag);
						c.addView(tag);
					} else {
						c.removeView(map.get(name));
						map.remove(name);
					}
				} catch (Exception x) { }
			}
		});
		if (!name.contains("@"))
		{
			int idx = name.indexOf('$');
			String ns = name.substring(0, idx);
			String sym = name.substring(idx + 1);
			lua.queueLuaCode(ns + "_" + (v ? "enter" : "leave") + "(\"" + sym + "\")");
		}
	}	
	
	@Override
	public void onCreate() {
		Singletons.sc.register(this);
		Singletons.scHandler.register(handler = new Handler());
		
		msger = new Messenger(handler);
		lua = new DemoLuaRuntime(this);
		
		lua.onCreate();

		nativeContextRef = new HashMap<String, Integer>();

		wifi = new WifiContext(this);
		wifi.onCreate();
		try {
			InputStream is;
			try {
				is = openFileInput("wifi_context.txt");
			} catch (Exception x) {
                // Fallback file location
				is = getAssets().open("wifi_context.txt");
			}
			wifi.addRulesFromReader(new InputStreamReader(is));
		} catch (Exception x) {
			x.printStackTrace();
		}
		
		motion = new MotionContext(this);
		motion.onCreate();
		
		sound = new SoundContext(this);
		sound.onCreate();
		
		eng = new ContextInferenceEngine();
		try {
			InputStream is;
			try {
				is = openFileInput("symbol_rules.txt");
			} catch (Exception x) {
                // Fallback file location
				is = getAssets().open("symbol_rules.txt");
			}
			eng.init();
			initContextEng(new InputStreamReader(is));
			Iterator<String> it = eng.symbols.keySet().iterator();
			while (it.hasNext()) {
				String name = it.next();
				if (eng.symbols.get(name))
					notifyContext(name);
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		
		wifi.bind();
		motion.bind();
		sound.bind();
		
		Util.log("SmartContext service created");
		
		lua.queueLuaCode("require \"demo\"");
		
		resetUI(null);
	}
	
	@Override
	public void onDestroy() {
		Util.log("Destroying the SmartContext service");
		
		Singletons.sc.clear();
		Singletons.scHandler.clear();
		
		lua.onDestroy();
		
		wifi.unbind();
		motion.unbind();
		sound.unbind();
		
		wifi.onDestroy();
		motion.onDestroy();
		sound.onDestory();
	}
	
	void initContextEng(Reader _in) {
		BufferedReader in = new BufferedReader(_in);
		HashSet<String> namespaces = new HashSet<String>();
		while (true) {
			String line;
			try { line = in.readLine(); } catch (Exception x) { line = null; }
			if (line == null) break;
			int pos = line.indexOf(',');
			if (pos == -1 || pos == 0 || pos == line.length() - 1) break;
			String nspc = Util.decodeURLString(line.substring(0, pos));
			String path = Util.decodeURLString(line.substring(pos + 1));
			if (namespaces.contains(nspc)) continue;
			Util.log("Processing rule file " + path + " with namespace " + nspc);
			InputStream is;
			try { is = openFileInput(path); } catch (Exception x) { is = null; }
			
			if (is == null) {
				try { is = getAssets().open(path); } catch (Exception x) { is = null; }
			}
			if (is == null) {
				try { is = new FileInputStream(path); } catch (Exception x) { is = null; }
			}
			if (is == null) continue;
			eng.parseReader(nspc, new InputStreamReader(is));
		}
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
	
	public void resetUI(final Bundle args) {
		Iterator<String> it = eng.symbols.keySet().iterator();
		
		final ArrayList<String> ctx_list = new ArrayList<String>();
		while (it.hasNext()) {
			String k = it.next();
			boolean v = eng.symbols.get(k);
			if (v) ctx_list.add(k);
		}
		
		final ArrayList<String> wifi_symbol_list = new ArrayList<String>();
		Iterator<WifiRule> rule_it = wifi.rules.iterator();
		HashSet<String> wifi_symbol = new HashSet<String>();
		
		while (rule_it.hasNext()) {
			WifiRule r = rule_it.next();
			wifi_symbol.add(r.result_context);
		}
		
		it = wifi_symbol.iterator();
		while (it.hasNext()) {
			String k = it.next();
			wifi_symbol_list.add(k);
		}
		
		final ArrayList<String> wifi_symbol_ssid_list;
		final ArrayList<String> wifi_scan_result;
		if (args != null && args.containsKey("wifiSymbolName")) {
			String name = args.getString("wifiSymbolName");
			
			wifi_symbol_ssid_list = new ArrayList<String>();
			wifi_scan_result = new ArrayList<String>();
			
			Iterator<WifiRule> rit = wifi.rules.iterator();
			while (rit.hasNext()) {
				WifiRule r = rit.next();
				if (!r.result_context.equals(name)) continue;
				wifi_symbol_ssid_list.add(r.ssid);
			}
			
			if (wifi.wifiLastScanResult != null) {
				Iterator<ScanResult> sit = wifi.wifiLastScanResult.iterator();
				while (sit.hasNext()) {
					ScanResult sr = sit.next();
					wifi_scan_result.add(sr.SSID);
				}
			}
		} else {
			wifi_symbol_ssid_list = null;
			wifi_scan_result = null;
		}
		
		Util.runInUIThread(new Runnable() {
			@Override
			public void run() {
				try {
					final EntryActivity a = Singletons.ea.get();
					if (a != null && a.isResumed) {
						a.contextViews.clear();
						a.contextViewContainer.removeAllViews();
						
						Iterator<String> it = ctx_list.iterator();
						while (it.hasNext()) {
							String k = it.next();
							TextView tag = new TextView(a);
							tag.setText(k);
							a.contextViews.put(k, tag);
							a.contextViewContainer.addView(tag);
						}
						
						a.wifiSymbolsContainer.removeAllViews();
						it = wifi_symbol_list.iterator();
						while (it.hasNext()) {
							final String wifiSymbolName = it.next();
							
							LinearLayout ll = new LinearLayout(a);
							ll.setOrientation(LinearLayout.HORIZONTAL);
							
							TextView tag = new TextView(a);
							tag.setText(wifiSymbolName);
							tag.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
							ll.addView(tag);
							
							Button editBtn = new Button(a);
							editBtn.setText("EDIT");
							editBtn.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0));
							ll.addView(editBtn);
							
							Button rmvBtn = new Button(a);
							rmvBtn.setText("DEL");
							rmvBtn.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0));
							ll.addView(rmvBtn);
							
							editBtn.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent i = new Intent(a, WifiRuleActivity.class);
									i.putExtra("symbolName", wifiSymbolName);
									a.startActivity(i);
								}
							});
							
							rmvBtn.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Util.runInSCThread(new Runnable() {
										public void run() {
											wifi.removeRules(wifiSymbolName, null);
											wifi.saveRulesToLocal();
											resetUI(null);
										}
									});
								}
							});
							a.wifiSymbolsContainer.addView(ll);
						}
					}
					
					final WifiRuleActivity b = Singletons.wra.get();
					if (b != null && b.isResumed &&
						args != null && args.containsKey("wifiSymbolName") && 
						args.getString("wifiSymbolName") == b.symbolName) {
						
						b.wifiNamesContainer.removeAllViews();
						
						Iterator<String> it = wifi_symbol_ssid_list.iterator();
						while (it.hasNext()) {
							final String ssid = it.next();
							
							LinearLayout ll = new LinearLayout(a);
							ll.setOrientation(LinearLayout.HORIZONTAL);
							
							TextView tag = new TextView(b);
							tag.setText(ssid);
							tag.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
							ll.addView(tag);
							
							Button rmvBtn = new Button(a);
							rmvBtn.setText("DEL");
							rmvBtn.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0));
							ll.addView(rmvBtn);
							
							rmvBtn.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Util.runInSCThread(new Runnable() {
										public void run() {
											wifi.removeRules(b.symbolName, ssid);
											wifi.saveRulesToLocal();
											Bundle args = new Bundle();
											args.putString("wifiSymbolName", b.symbolName);
											resetUI(args);
										}
									});
								}
							});
							
							b.wifiNamesContainer.addView(ll);
						}
						
						b.wifiSignalList.setAdapter(
								new ArrayAdapter<String>(b, android.R.layout.simple_list_item_1,
										wifi_scan_result.toArray(new String[0])));
					}
				} catch (Exception x) {
					x.printStackTrace();
				}
			}
		});
	}
}
