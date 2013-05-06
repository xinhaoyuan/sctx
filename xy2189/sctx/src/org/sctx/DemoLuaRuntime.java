package org.sctx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Handler;

public class DemoLuaRuntime {

	Context ctx;
	LuaState L;
	Handler handler;
	AudioManager am;
	
	class DemoLuaLibrary {
		public void print(String line) {
			Util.log("from lua: " + line);
		}
		
		public void setRingVolume(int v) {
			if (v == -2)
			{
				am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			}
			else if (v == -1) 
			{
				am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			}
			else if (v > 0) {
				am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
				v = (int)(v / 100.0 * am.getStreamMaxVolume(AudioManager.STREAM_RING));
				am.setStreamVolume(AudioManager.STREAM_RING, v, 0);
			}
		}
		
		public int getRingVolume() {
			int m = am.getRingerMode();
			if (m == AudioManager.RINGER_MODE_SILENT) return -2;
			else if (m == AudioManager.RINGER_MODE_VIBRATE) return -1;
			
			return (int)(am.getStreamVolume(AudioManager.STREAM_RING) / am.getStreamMaxVolume(AudioManager.STREAM_RING));
		}
	};
	
	DemoLuaLibrary lib = new DemoLuaLibrary();
	
	public DemoLuaRuntime(Context ctx) {
		this.ctx = ctx;
		handler = new Handler();
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
	
	public void onCreate() {
		am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		L = LuaStateFactory.newLuaState();
		L.openLibs();
		
		try {
			L.pushJavaObject(lib);
			L.setGlobal("lib");
			
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
					AssetManager am = ctx.getAssets();
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
			String customPath = ctx.getFilesDir() + "/?.lua";
			L.pushString(";" + customPath);    // package path custom
			L.concat(2);                       // package pathCustom
			L.setField(-2, "path");            // package
			L.pop(1);
		} catch (Exception e) {
			Util.log("Error while overriding LUA functions");
		}
	}
	
	public void onDestroy() {
		L.close();
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
	
	public void queueLuaCode(final String src) {
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
	
}
