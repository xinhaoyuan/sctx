package org.sctx;

import java.util.HashSet;

import android.os.Handler;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class WifiRuleActivity extends Activity {

	boolean isResumed = false;
	String symbolName;
	
	LinearLayout wifiNamesContainer;
	Spinner wifiSignalList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Singletons.wra.register(this);
		Singletons.uiHandler.tryRegister(new Handler());
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi_rules);
		
		wifiNamesContainer = (LinearLayout)findViewById(R.id.wifiNamesContainer);
		wifiSignalList = (Spinner)findViewById(R.id.WifiSignalList);
		
		Bundle args = getIntent().getExtras();
		symbolName = args.containsKey("symbolName") ? args.getString("symbolName") : "Wifi@Home";
		
		Button addBtn = (Button)findViewById(R.id.AddWifiSignal);
		addBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = (String)wifiSignalList.getSelectedItem();
				final WifiRule r = new WifiRule();
				r.ssid = name;
				r.list_type = WifiRule.LIST_TYPE_BLACK;
				r.essid_list = new HashSet<String>();
				r.level_threshold = -200;
				r.result_context = symbolName;
				Util.runInSCThread(new Runnable() {
					public void run() {
						SmartContext sc = Singletons.sc.get();
						if (sc != null) { 
							sc.wifi.addRule(r);
							Bundle args = new Bundle();
							args.putString("wifiSymbolName", symbolName);
							sc.wifi.saveRulesToLocal();
							sc.resetUI(args);
						}
					}
				});
			}
		});
		
		restoreState(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		Singletons.wra.clear();
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		final Bundle args = new Bundle();
		args.putString("wifiSymbolName", symbolName);
		
		Util.runInSCThread(new Runnable() {
			@Override
			public void run() {
				SmartContext sc = Singletons.sc.get();
				if (sc != null) 
					sc.resetUI(args);
			}
		});
		
		isResumed = true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		isResumed = false;
	}
	
	void saveState(Bundle state) {
    	state.putString("symbolName", symbolName);
    }
    
    void restoreState(Bundle state) {
    	if (state != null && state.containsKey("symbolName")) {
    		symbolName = state.getString("symbolName");
    	}
    	((TextView)findViewById(R.id.WifiRuleName)).setText("Wifi for Rule \"" + symbolName + "\"");
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
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.wifi_rules, menu);
//		return true;
//	}

}
