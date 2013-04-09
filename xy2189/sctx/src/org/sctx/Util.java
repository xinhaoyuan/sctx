package org.sctx;

import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

class Util {
	public static void log(final String msg) {
		Log.i("Util.log", msg);
		
		runInUIThread(new Runnable() {			
			@Override
			public void run() {
				TextView text = EntryActivity.singleton.logText;
				try {
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
}
