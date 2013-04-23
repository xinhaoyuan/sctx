package org.sctx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

public class SmartContext extends Service {
	
	Handler handler;
	Messenger msger;
	
	WifiContext wifi;
	MotionContext motion;
	SoundContext sound;
	
	LuaState L;
	
	HashMap<String, Integer> contextExternalRef;
	HashMap<String, Integer> contextInternalRef;
	
	void getExternalContext(String name) {
		if (contextExternalRef.containsKey(name)) {
			contextExternalRef.put(name, contextExternalRef.get(name) + 1);
		}
		else {
			contextExternalRef.put(name, 1);
			enterContext(name);
		}
	}
	
	void putExternalContext(String name) {
		if (contextExternalRef.containsKey(name)) {
			int value = contextExternalRef.get(name);
			if (value == 1)
			{
				contextExternalRef.remove(name);
				leaveContext(name);
			} else {
				contextExternalRef.put(name, value - 1);
			}
		}
	}
	
	void enterContext(String name) {
		Util.log("Enter context " + name);
	}
	
	void leaveContext(String name) {
		Util.log("Leave context " + name);
	}
	
	@Override
	public void onCreate() {
		handler = new Handler();
		msger = new Messenger(handler);
		
		initLua();
		
		contextExternalRef = new HashMap<String, Integer>();
		contextInternalRef = new HashMap<String, Integer>();
		
		wifi = new WifiContext(this);
		wifi.onCreate();
		
		motion = new MotionContext(this);
		motion.onCreate();
		
		sound = new SoundContext(this);
		sound.onCreate();
		
		WifiRule rule = new WifiRule();
		rule.ssid = "Columbia University";
		rule.list_type = WifiRule.LIST_TYPE_BLACK;
		rule.level_threshold = -100;
		rule.result_context = "School";
		wifi.addRule(rule);
		
		wifi.bind();
		motion.bind();
		sound.bind();
		
		Util.log("SmartContext service created");
		
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
}
