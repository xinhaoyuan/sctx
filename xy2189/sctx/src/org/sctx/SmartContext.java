package org.sctx;

import android.app.IntentService;
import android.content.Intent;

public class SmartContext extends IntentService {
	
	boolean initFlag = false;
	
	public SmartContext() {
		super("SmartContext");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (action.equals("init")) {
			initialize();
		}
	}
	
	void initialize() {
		if (initFlag) return;
		
		// Do initialization here
		
		Util.log("service initialized\n");
		initFlag = true;
	}

}
