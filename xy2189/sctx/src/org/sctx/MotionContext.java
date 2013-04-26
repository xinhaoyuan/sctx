package org.sctx;

import java.util.HashSet;
import java.util.Iterator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;

public class MotionContext {
	boolean active = false;
	
	static final String sensor_service = Context.SENSOR_SERVICE;
	SensorManager sensorManager;
	AccMonitorRunnable monitor;
	
	Sensor acc;
	int accSamplingInterval = 10000;
	int accSamplingLength = 200;
	
	class ListenerNode {
		SensorEventListener listener;
		Sensor sensor;
		
		public ListenerNode(SensorEventListener l, Sensor s) {
			listener = l; sensor = s;
		}
	};
	
	HashSet<ListenerNode> listening;
	
	SmartContext ctx;
	MotionContext(SmartContext ctx) {
		this.ctx = ctx;
	}
	
	void onCreate() {
		sensorManager = (SensorManager)ctx.getSystemService(sensor_service);
		acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		listening = new HashSet<ListenerNode>();
	}
	
	void bind() {
		if (monitor == null) {
			monitor = new AccMonitorRunnable();
			ctx.handler.post(monitor);
		}
	}
	
	void unbind() {
		monitor.active = false;
		monitor = null;
	}
	
	void setLinearAccContext(float x, float y, float z) {
		double dis = Math.sqrt(x * x + y * y + z * z);
		Util.log("Linear Acc: " + dis); 
	}
	
	class AccMonitorRunnable implements Runnable {
		boolean active = true;
		
		@Override
		public void run() {
			final AccMonitorRunnable self = this;
			final AccListener accListener = new AccListener();
			final ListenerNode node = new ListenerNode(accListener, acc);
			sensorManager.registerListener(accListener, acc, SensorManager.SENSOR_DELAY_GAME);
			listening.add(node);
			ctx.handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (listening.contains(node)) {
						listening.remove(node);
						sensorManager.unregisterListener(accListener, acc);
					}
					
					if (!active) return;
					
					setLinearAccContext(accListener.linear_acc[0], accListener.linear_acc[1], accListener.linear_acc[2]);
					// setLinearAccContext(accListener.gravity[0], accListener.gravity[1], accListener.gravity[2]);
					ctx.handler.postDelayed(self, accSamplingInterval);
				}
			}, accSamplingLength);
		}
		
		class AccListener implements SensorEventListener {
			float[] gravity = new float[3];
			float[] linear_acc = new float[3];
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}

			@Override
			public void onSensorChanged(SensorEvent event) {
				final float alpha = 0.8f; 
				for (int i = 0; i < 3; ++ i)
					gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
				for (int i = 0; i < 3; ++ i)
					linear_acc[i] = event.values[i] - gravity[i];
			}
		}
	};
	
	void onDestroy() {
		Iterator<ListenerNode> it = listening.iterator();
		while (it.hasNext()) {
			ListenerNode node = it.next();
			sensorManager.unregisterListener(node.listener, node.sensor);
		}
		listening.clear();
	}
}
