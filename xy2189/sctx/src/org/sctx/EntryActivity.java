package org.sctx;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

    void setTabBtn(final int btnId, final ViewGroup parent, final int childId) {
    	Button tabBtn = (Button)findViewById(btnId);
		tabBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int idx = 0; idx < parent.getChildCount(); ++ idx) {
					View c = parent.getChildAt(idx);
					c.setVisibility(c.getId() == childId ? View.VISIBLE : View.INVISIBLE);
				}
			}
		});
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	if (singleton != null) {
    		throw new RuntimeException("EntryActivity already exists");
    	}
    	
    	if (handler == null) handler = new Handler();
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        logText = (TextView)findViewById(R.id.LogText);
        ViewGroup tabView = (ViewGroup)findViewById(R.id.TabLayout);
        
		singleton = this;
		
		logText.setKeyListener(null);
		logText.setTextIsSelectable(true);
		
		Button startBtn = (Button)findViewById(R.id.StartBtn);
		startBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("Initializing service");
				Intent intent = new Intent(v.getContext(), SmartContext.class);
				intent.setAction("init");
		        startService(intent);
			}
		});
		
		Button stopBtn = (Button)findViewById(R.id.StopBtn);
		stopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("Stoping service");
				Intent intent = new Intent(v.getContext(), SmartContext.class);
				intent.setAction("init");
		        stopService(intent);
			}
		});
		
		Button clearBtn = (Button)findViewById(R.id.ClearBtn);
		clearBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				logText.setText("");
			}
		});
		
		setTabBtn(R.id.LogTabBtn, tabView, R.id.LogLayout);
		setTabBtn(R.id.WifiRuleTabBtn, tabView, R.id.WifiRuleLayout);
		
		for (int i = 0; i < tabView.getChildCount(); ++ i)
			tabView.getChildAt(i).setVisibility(i == 0 ? View.VISIBLE : View.INVISIBLE);
    }
    
    
    @Override
    public void onDestroy() {
    	singleton = null;
    	super.onDestroy();
    }
}
