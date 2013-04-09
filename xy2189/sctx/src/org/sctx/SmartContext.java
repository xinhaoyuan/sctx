package org.sctx;

import java.util.HashMap;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

public class SmartContext extends Service {
	
	Handler handler;
	Messenger msger;
	
	WifiContext wifi;
	
	HashMap<String, Integer> contextRef;
	
	void getContext(String name) {
		if (contextRef.containsKey(name)) {
			contextRef.put(name, contextRef.get(name) + 1);
		}
		else {
			contextRef.put(name, 1);
			enterContext(name);
		}
	}
	
	void putContext(String name) {
		if (contextRef.containsKey(name)) {
			int value = contextRef.get(name);
			if (value == 1)
			{
				contextRef.remove(name);
				leaveContext(name);
			} else {
				contextRef.put(name, value - 1);
			}
		}
	}
	
	void enterContext(String name) {
		Util.log("Enter context " + name);
	}
	
	void leaveContext(String name) {
		Util.log("Leave context " + name);
	}
	
	@Override
	public void onCreate() {
		handler = new Handler();
		msger = new Messenger(handler);
		
		contextRef = new HashMap<String, Integer>();
		
		wifi = new WifiContext(this);
		wifi.onCreate();
		
		WifiRule rule = new WifiRule();
		rule.ssid = "Columbia University";
		rule.list_type = rule.LIST_TYPE_BLACK;
		rule.level_threshold = -100;
		rule.result_context = "School";
		wifi.addRule(rule);
		
		wifi.bind();
		
		Util.log("SmartContext service created");
	}
	
	@Override
	public void onDestroy() {
		Util.log("Destroying the SmartContext service");
		
		wifi.unbind();
		wifi.onDestroy();
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
	
	
}
