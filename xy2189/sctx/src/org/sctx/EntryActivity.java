package org.sctx;

import android.app.Activity;
import android.os.Handler;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class EntryActivity extends Activity
{
	static EntryActivity singleton;
	static Handler handler;
	
	TextView logText;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	if (singleton != null) {
    		throw new RuntimeException("EntryActivity already exists");
    	}
    	
    	if (handler == null) handler = new Handler();
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        logText = (TextView)findViewById(R.id.logText);
        
		singleton = this;
		
		Util.log("Initializing service\n");
		
		Intent intent = new Intent(this, SmartContext.class);
		intent.setAction("init");
        startService(intent);
    }
    
    
    @Override
    public void onDestroy() {
    	singleton = null;
    	super.onDestroy();
    }
}
