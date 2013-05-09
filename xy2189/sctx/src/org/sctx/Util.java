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
					EntryActivity ea = Singletons.ea.get();
					TextView text = ea.logText;
					text.append(msg + "\n");
				} catch (Exception x) { }
			}
		});
	}
	
	public static boolean runInUIThread(Runnable r) {
		Handler handler = Singletons.uiHandler.get();
		if (handler == null) return false;
		handler.post(r);
		return true;
	}
	
	public static boolean runInSCThread(Runnable r) {
		Handler handler = Singletons.scHandler.get();
		if (handler == null) return false;
		handler.post(r);
		return true;
	}
	
	public static boolean runInSCThreadDelayed(Runnable r, long delay) {
		Handler handler = Singletons.scHandler.get();
		if (handler == null) return false;
		handler.postDelayed(r, delay);
		return true;
	}
	
	public static String decodeURLString(String s) {
		if (s == null) return null;
		try {
			return URLDecoder.decode(s, "utf-8");
		} catch (Exception x) {
			return null;
		}
	}
	
	public static String encodeURLString(String s) {
		if (s == null) return null;
		try {
			return URLEncoder.encode(s, "utf-8");
		} catch (Exception x) {
			return null;
		}
	}
}

class SingletonHolder<T> {
	T i;
	
	synchronized T get() {
		return i;
	}
	
	synchronized void register(T i) {
		if (this.i != null && this.i != i)
			throw new RuntimeException("Cannot set singleton holder twice");
		this.i = i;
	}
	
	synchronized boolean tryRegister(T i) {
		if (this.i == null) { 
			this.i = i;
			return true;
		}
		else return false;
	}
	
	synchronized void clear() {
		if (i == null)
			throw new RuntimeException("Cannot clear empty singleton holder");
		i = null;
	}
}

class Singletons {
	static SingletonHolder<Handler> scHandler;
	static SingletonHolder<Handler> uiHandler;
	static SingletonHolder<SmartContext> sc;
	static SingletonHolder<EntryActivity> ea;
	static SingletonHolder<WifiRuleActivity> wra;
}
