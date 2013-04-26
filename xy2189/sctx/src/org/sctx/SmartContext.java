package org.sctx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SmartContext extends Service {
	
	static SmartContext singleton = null;
	
	Handler handler;
	Messenger msger;
	
	WifiContext wifi;
	MotionContext motion;
	SoundContext sound;
	
	LuaState L;
	
	HashMap<String, Integer> nativeContextRef;
	
	ContextInferenceEngine eng;
	
	void getNativeContext(String name) {
		if (nativeContextRef.containsKey(name)) {
			nativeContextRef.put(name, nativeContextRef.get(name) + 1);
		}
		else {
			nativeContextRef.put(name, 1);
			enterContext(name);
		}
	}
	
	void putNativeContext(String name) {
		if (nativeContextRef.containsKey(name)) {
			int value = nativeContextRef.get(name);
			if (value == 1)
			{
				nativeContextRef.remove(name);
				leaveContext(name);
			} else {
				nativeContextRef.put(name, value - 1);
			}
		}
	}
	
	void enterContext(String name) {
		ArrayList<String> updateList = new ArrayList<String>();
		eng.updateSystemSymbol(name, true, updateList);
		Iterator<String> it = updateList.iterator();
		while (it.hasNext()) {
			String symbol = it.next();
			notifyContext(symbol);
		}
	}
	
	void leaveContext(String name) {
		ArrayList<String> updateList = new ArrayList<String>();
		eng.updateSystemSymbol(name, false, updateList);
		Iterator<String> it = updateList.iterator();
		while (it.hasNext()) {
			String symbol = it.next();
			notifyContext(symbol);
		}
	}
	
	void notifyContext(final String name) {
		final boolean v = eng.symbols.get(name);
		Util.runInUIThread(new Runnable() {
			@Override
			public void run() {
				try {
					EntryActivity a = EntryActivity.singleton;
					HashMap<String, TextView> map = a.contextViews;
					LinearLayout c = a.contextViewContainer;
					
					if (v) {
						TextView tag = new TextView(a);
						tag.setText(name);
						map.put(name, tag);
						c.addView(tag);
					} else {
						c.removeView(map.get(name));
						map.remove(name);
					}
				} catch (Exception x) { }
			}
		});
		if (v)
			Util.log("Enter context " + name);
		else Util.log("Leave context " + name);
	}	
	
	@Override
	public void onCreate() {
		if (singleton != null) throw new RuntimeException("SmartContext is already running");
		singleton = this;
		
		handler = new Handler();
		msger = new Messenger(handler);
		
		Util.runInUIThread(new Runnable() {
			@Override
			public void run() {
				try {
					EntryActivity a = EntryActivity.singleton;
					a.contextViews.clear();
					a.contextViewContainer.removeAllViews();
				} catch (Exception x) { }
			}
		});
		
		initLua();
		
		nativeContextRef = new HashMap<String, Integer>();

		wifi = new WifiContext(this);
		wifi.onCreate();
		try {
			InputStream is;
			try {
				is = openFileInput("wifi_context.txt");
			} catch (Exception x) {
                // Fallback file location
				is = getAssets().open("wifi_context.txt");
			}
			wifi.addRulesFromReader(new InputStreamReader(is));
		} catch (Exception x) {
			x.printStackTrace();
		}
		
		motion = new MotionContext(this);
		motion.onCreate();
		
		sound = new SoundContext(this);
		sound.onCreate();
		
		eng = new ContextInferenceEngine();
		try {
			InputStream is;
			try {
				is = openFileInput("symbol_rules.txt");
			} catch (Exception x) {
                // Fallback file location
				is = getAssets().open("symbol_rules.txt");
			}
			eng.init(new InputStreamReader(is));
			Iterator<String> it = eng.symbols.keySet().iterator();
			while (it.hasNext()) {
				String name = it.next();
				if (eng.symbols.get(name))
					notifyContext(name);
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		
		wifi.bind();
		motion.bind();
		sound.bind();
		
		Util.log("SmartContext service created");

        // For testing the LUA engine
		queueLuaCode("print \"Hello world\"");
	}
	
	@Override
	public void onDestroy() {
		Util.log("Destroying the SmartContext service");
		
		L.close();
		
		wifi.unbind();
		motion.unbind();
		sound.unbind();
		
		wifi.onDestroy();
		motion.onDestroy();
		sound.onDestory();
		
		singleton = null;
	}
	
	private static byte[] readAll(InputStream input) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
		byte[] buffer = new byte[4096];
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}
	
	void initLua() {
		L = LuaStateFactory.newLuaState();
		L.openLibs();
		
		try {
			JavaFunction print = new JavaFunction(L) {
				@Override
				public int execute() throws LuaException {
					StringBuilder output = new StringBuilder();
					for (int i = 2; i <= L.getTop(); i++) {
						int type = L.type(i);
						String stype = L.typeName(type);
						String val = null;
						if (stype.equals("userdata")) {
							Object obj = L.toJavaObject(i);
							if (obj != null)
								val = obj.toString();
						} else if (stype.equals("boolean")) {
							val = L.toBoolean(i) ? "true" : "false";
						} else {
							val = L.toString(i);
						}
						if (val == null)
							val = stype;						
						output.append(val);
						output.append("\t");
					}
					Util.log(output.toString());					
					return 0;
				}
			};
			print.register("print");
			
			JavaFunction assetLoader = new JavaFunction(L) {
				@Override
				public int execute() throws LuaException {
					String name = L.toString(-1);

					AssetManager am = getAssets();
					try {
						InputStream is = am.open(name + ".lua");
						byte[] bytes = readAll(is);
						L.LloadBuffer(bytes, name);
						return 1;
					} catch (Exception e) {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						e.printStackTrace();
						L.pushString("Cannot load module "+name+":\n"+os.toString());
						return 1;
					}
				}
			};
			
			L.getGlobal("package");            // package
			L.getField(-1, "loaders");         // package loaders
			int nLoaders = L.objLen(-1);       // package loaders
			
			L.pushJavaFunction(assetLoader);   // package loaders loader
			L.rawSetI(-2, nLoaders + 1);       // package loaders
			L.pop(1);                          // package
						
			L.getField(-1, "path");            // package path
			String customPath = getFilesDir() + "/?.lua";
			L.pushString(";" + customPath);    // package path custom
			L.concat(2);                       // package pathCustom
			L.setField(-2, "path");            // package
			L.pop(1);
		} catch (Exception e) {
			Util.log("Error while overriding LUA functions");
		}
	}
	
	private String errorReason(int error) {
		switch (error) {
		case 4:
			return "Out of memory";
		case 3:
			return "Syntax error";
		case 2:
			return "Runtime error";
		case 1:
			return "Yield error";
		}
		return "Unknown error " + error;
	}
	
	void queueLuaCode(final String src) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				L.setTop(0);
				int ok = L.LloadString(src);
				if (ok == 0) {
					L.getGlobal("debug");
					L.getField(-1, "traceback");
					L.remove(-2);
					L.insert(-2);
					ok = L.pcall(0, 0, -2);
					if (ok == 0) {
						return;
					}
				}
				String msg = errorReason(ok) + ": " + L.toString(-1);
				Util.log("LUA error: " + msg);
			}
			
		});
	}
	
	@SuppressLint("HandlerLeak")
	class RequestHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Util.log("Got request what = " + msg.what);
		}
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return msger.getBinder();
    }
	
	public void resetUI() {
		Iterator<String> it = eng.symbols.keySet().iterator();
		final ArrayList<String> list = new ArrayList<String>();
		while (it.hasNext()) {
			String k = it.next();
			boolean v = eng.symbols.get(k);
			if (v) list.add(k);
		}
		
		Util.runInUIThread(new Runnable() {
			@Override
			public void run() {
				try {
					EntryActivity a = EntryActivity.singleton;
					a.contextViews.clear();
					a.contextViewContainer.removeAllViews();
					
					Iterator<String> it = list.iterator();
					while (it.hasNext()) {
						String k = it.next();
						TextView tag = new TextView(a);
						tag.setText(k);
						a.contextViews.put(k, tag);
						a.contextViewContainer.addView(tag);
					}
				} catch (Exception x) { }
			}
		});
	}
}
