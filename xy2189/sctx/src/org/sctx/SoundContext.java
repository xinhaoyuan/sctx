package org.sctx;

import android.media.MediaRecorder;
import android.util.Log;

public class SoundContext {
	
	SmartContext ctx;
	int samplingInterval = 200;
	SoundMonitorRunnable monitor = null;
	MediaRecorder recorder = null;
	
	SoundContext(SmartContext ctx) {
		this.ctx = ctx;
	}
	
	void onCreate() {
		if (recorder == null) {
			try {
				recorder = new MediaRecorder();
				recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				recorder.setOutputFile("/dev/null");
				recorder.prepare();
				recorder.start();
			} catch (Exception x) {
				x.printStackTrace();
				recorder = null;
			}
		}
	}
	
	void onDestory() {
		if (recorder != null) {
			recorder.stop();
			recorder.release();
			recorder = null;
		}
	}
	
	void bind() {
		if (monitor == null) {
			monitor = new SoundMonitorRunnable();
			ctx.handler.post(monitor);
		}
	}
	
	void unbind() {
		monitor.active = false;
		monitor = null;
	}

	class SoundMonitorRunnable implements Runnable {
		boolean active = true;
		double amp_filtered = 0.0;
		
		@Override
		public void run() {
			double amp = 0;
			if (recorder != null) {
				amp = recorder.getMaxAmplitude();
			}
			amp_filtered = amp_filtered * 0.7 + 0.3 * amp;
			
			Util.log("AMP: " + amp_filtered);
			
			if (active) ctx.handler.postDelayed(this, samplingInterval);
		}
		
	}
}
