package org.sctx;

import java.net.URLDecoder;

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
		if (EntryActivity.handler == null) return false;
		EntryActivity.handler.post(r);
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
}
