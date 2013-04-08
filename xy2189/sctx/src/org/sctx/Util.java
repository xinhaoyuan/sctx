package org.sctx;

import android.util.Log;

class Util {
	public static void log(final String msg) {
		Log.i("Util.log", msg);
		
		runInUIThread(new Runnable() {			
			@Override
			public void run() {
				try {
					EntryActivity.singleton.logText.append(msg);
				} catch (Exception x) { }
			}
		});
	}
	
	public static boolean runInUIThread(Runnable r) {
		if (EntryActivity.handler == null) return false;
		EntryActivity.handler.post(r);
		return true;
	}
}
