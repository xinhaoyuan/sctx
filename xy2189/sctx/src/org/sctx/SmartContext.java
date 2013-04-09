package org.sctx;

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
	
	@Override
	public void onCreate() {
		handler = new Handler();
		msger = new Messenger(handler);
		
		wifi = new WifiContext(this);
		wifi.onCreate();
		
		Util.log("SmartContext service created");
	}
	
	@Override
	public void onDestroy() {
		Util.log("Destroying the SmartContext service");
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
