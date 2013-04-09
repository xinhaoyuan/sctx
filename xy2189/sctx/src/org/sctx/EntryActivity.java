package org.sctx;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
		
		Button startBtn = (Button)findViewById(R.id.startBtn);
		startBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("Initializing service");
				Intent intent = new Intent(v.getContext(), SmartContext.class);
				intent.setAction("init");
		        startService(intent);
			}
		});
		
		Button stopBtn = (Button)findViewById(R.id.stopBtn);
		stopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("Stoping service");
				Intent intent = new Intent(v.getContext(), SmartContext.class);
				intent.setAction("init");
		        stopService(intent);
			}
		});
    }
    
    
    @Override
    public void onDestroy() {
    	singleton = null;
    	super.onDestroy();
    }
}
