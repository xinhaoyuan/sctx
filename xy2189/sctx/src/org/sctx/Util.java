package org.sctx;

import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.http.client.utils.URLEncodedUtils;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

class Util {
	public static void log(final String msg) {
		Log.i("Util.log", msg);
		
		runInUIThread(new Runnable() {			
			@Override
			public void run() {
				try {
					TextView text = EntryActivity.singleton.logText;
					text.append(msg + "\n");
				} catch (Exception x) { }
			}
		});
	}
	
	public static boolean runInUIThread(Runnable r) {
		Handler handler;
		synchronized (EntryActivity.class) {
			handler = EntryActivity.handler;
		}
		if (handler == null) return false;
		handler.post(r);
		return true;
	}
	
	public static boolean runInSCThread(Runnable r) {
		Handler handler;
		synchronized (SmartContext.class) {
			handler = SmartContext.handler;
		}
		if (handler == null) return false;
		handler.post(r);
		return true;
	}
	
	public static boolean runInSCThreadDelayed(Runnable r, long delay) {
		Handler handler;
		synchronized (SmartContext.class) {
			handler = SmartContext.handler;
		}
		if (handler == null) return false;
		handler.postDelayed(r, delay);
		return true;
	}
	
	public static String decodeString(String s) {
		if (s == null) return null;
		try {
			return URLDecoder.decode(s, "utf-8");
		} catch (Exception x) {
			return null;
		}
	}
	
	public static String encodeString(String s) {
		if (s == null) return null;
		try {
			return URLEncoder.encode(s, "utf-8");
		} catch (Exception x) {
			return null;
		}
	}
}
