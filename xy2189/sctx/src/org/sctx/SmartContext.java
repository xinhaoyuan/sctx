package org.sctx;

import java.util.HashMap;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

public class SmartContext extends Service {
	
	Handler handler;
	Messenger msger;
	
	WifiContext wifi;
	MotionContext motion;
	
	HashMap<String, Integer> contextExternalRef;
	HashMap<String, Integer> contextInternalRef;
	
	void getExternalContext(String name) {
		if (contextExternalRef.containsKey(name)) {
			contextExternalRef.put(name, contextExternalRef.get(name) + 1);
		}
		else {
			contextExternalRef.put(name, 1);
			enterContext(name);
		}
	}
	
	void putExternalContext(String name) {
		if (contextExternalRef.containsKey(name)) {
			int value = contextExternalRef.get(name);
			if (value == 1)
			{
				contextExternalRef.remove(name);
				leaveContext(name);
			} else {
				contextExternalRef.put(name, value - 1);
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
		
		contextExternalRef = new HashMap<String, Integer>();
		contextInternalRef = new HashMap<String, Integer>();
		
		wifi = new WifiContext(this);
		wifi.onCreate();
		
		motion = new MotionContext(this);
		motion.onCreate();
		
		WifiRule rule = new WifiRule();
		rule.ssid = "Columbia University";
		rule.list_type = WifiRule.LIST_TYPE_BLACK;
		rule.level_threshold = -100;
		rule.result_context = "School";
		wifi.addRule(rule);
		
		wifi.bind();
		motion.bind();
		
		Util.log("SmartContext service created");
	}
	
	@Override
	public void onDestroy() {
		Util.log("Destroying the SmartContext service");
		
		wifi.unbind();
		motion.unbind();
		
		wifi.onDestroy();
		motion.onDestroy();
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
