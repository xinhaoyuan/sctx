package org.sctx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, SmartContext.class);
	    context.startService(i);
	}
}