package org.sctx;

import java.util.HashMap;

import android.app.Activity;
import android.telephony.SignalStrength;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.os.Handler;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class EntryActivity extends Activity
{
	static EntryActivity singleton;
	static Handler handler;
	
	boolean isResumed;
	TextView logText;
	LinearLayout contextViewContainer;
	LinearLayout wifiSymbolsContainer;
	HashMap<String, TextView> contextViews;
	
	ViewGroup tabView;
	int currentTabChildId;
	
    void setTabBtn(final int btnId, final ViewGroup parent, final int childId) {
    	Button tabBtn = (Button)findViewById(btnId);
		tabBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				currentTabChildId = childId;
				for (int idx = 0; idx < parent.getChildCount(); ++ idx) {
					View c = parent.getChildAt(idx);
					if (c.getId() == childId) { 
						c.setVisibility(View.VISIBLE);
					} else if (c.getVisibility() == View.VISIBLE) {
						c.setVisibility(View.INVISIBLE);
						View view = c.findFocus();
						if (view != null) view.clearFocus();
					}
					
				}
			}
		});
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	synchronized (EntryActivity.class) {
    		if (singleton != null) {
        		throw new RuntimeException("EntryActivity already exists");
        	}
    		singleton = this;
    		if (handler == null) handler = new Handler();
		}
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        logText = (TextView)findViewById(R.id.LogText);
        contextViewContainer = (LinearLayout)findViewById(R.id.ContextViewContainer);
        wifiSymbolsContainer = (LinearLayout)findViewById(R.id.WifiSymbolsContainer);
        contextViews = new HashMap<String, TextView>();
        
        tabView = (ViewGroup)findViewById(R.id.TabLayout);
		
		logText.setKeyListener(null);
		logText.setTextIsSelectable(true);
		
		Button startBtn = (Button)findViewById(R.id.StartBtn);
		startBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("Initializing service");
				Intent intent = new Intent(v.getContext(), SmartContext.class);
				intent.setAction("init");
		        startService(intent);
			}
		});
		
		final Button stopBtn = (Button)findViewById(R.id.StopBtn);
		stopBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("Stoping service");
				Intent intent = new Intent(v.getContext(), SmartContext.class);
				intent.setAction("init");
		        stopService(intent);
			}
		});
		
		Button resetConfBtn = (Button)findViewById(R.id.ResetConf);
		resetConfBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopBtn.performClick();
				// remove all local configuration files
				deleteFile("wifi_context.txt");
			}
		});
		
		Button clearBtn = (Button)findViewById(R.id.ClearBtn);
		clearBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				logText.setText("");
			}
		});
		
		Button addWifiBtn = (Button)findViewById(R.id.AddWifiSymbol);
		final TextView wifiSymbolInput = (TextView)findViewById(R.id.WifiSymbolInput);
		
		addWifiBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String wifiSymbolName = wifiSymbolInput.getText().toString();
				Intent i = new Intent(EntryActivity.this, WifiRuleActivity.class);
				i.putExtra("symbolName", "Wifi@" + wifiSymbolName);
				startActivity(i);
			}
		});
		
		setTabBtn(R.id.LogTabBtn, tabView, R.id.LogLayout);
		setTabBtn(R.id.ContextView, tabView, R.id.ContextViewLayout);
		setTabBtn(R.id.WifiSymbols, tabView, R.id.WifiSymbolsLayout);
		
		restoreState(savedInstanceState);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
		Util.runInSCThread(new Runnable() {
			@Override
			public void run() {
				if (SmartContext.singleton != null) 
					SmartContext.singleton.resetUI(null);
			}
		});
    	isResumed = true;
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	isResumed = false;
    }
    
    void saveState(Bundle state) {
    	state.putInt("currentTabChildId", currentTabChildId);
    }
    
    void restoreState(Bundle state) {
    	if (state != null && state.containsKey("currentTabChildId")) {
    		currentTabChildId = state.getInt("currentTabChildId");
    	} else {
    		currentTabChildId = R.id.LogLayout;
    	}
    	
    	for (int i = 0; i < tabView.getChildCount(); ++ i) {
			View v = tabView.getChildAt(i);
			v.setVisibility(v.getId() == currentTabChildId ? View.VISIBLE : View.INVISIBLE);
		}
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	saveState(outState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	restoreState(savedInstanceState);
    }
    
    @Override
    public void onDestroy() {
    	synchronized (EntryActivity.class) {
    		singleton = null;
    	}
    	super.onDestroy();
    }
}
