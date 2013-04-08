package org.sctx;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class EntryActivity extends Activity
{
	static EntryActivity singleton;
	TextView logText;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	if (singleton != null) {
    		throw new RuntimeException("EntryActivity already exists");
    	}
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        logText = (TextView)this.findViewById(R.id.logText);
        logText.append("Hello World");
		singleton = this;
    }
    
    @Override
    public void onDestroy() {
    	singleton = null;
    	super.onDestroy();
    }
}
