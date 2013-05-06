package org.sctx;

import android.media.MediaRecorder;
import android.util.Log;

public class SoundContext {
	
	SmartContext ctx;
	int samplingInterval = 5000;
	int samplingRate = 100;
	int samplingLength = 1000;
	
	SoundMonitorRunnable monitor = null;
	MediaRecorder recorder = null;
	double background_amp_level = 0.0;
	
	SoundContext(SmartContext ctx) {
		this.ctx = ctx;
	}
	
	void onCreate() {
		startRecording();
	}
	
	void onDestory() {
		
	}
	
	void bind() {
		startRecording();
		if (monitor == null) {
			monitor = new SoundMonitorRunnable();
			Util.runInSCThread(monitor);
		}
	}
	
	void unbind() {
		monitor.active = false;
		monitor = null;
		stopRecording();
	}
	
	void startRecording() {
		if (recorder == null) {
			try {
				recorder = new MediaRecorder();
				recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				recorder.setOutputFile("/dev/null");
				recorder.prepare();
				recorder.start();
				Log.i("DEBUG", "recorder initialized");
			} catch (Exception x) {
				x.printStackTrace();
				recorder = null;
			}
		}
	}
	
	void stopRecording() {
		if (recorder != null) {
			recorder.stop();
			recorder.release();
			recorder = null;
		}
	}

	class SoundMonitorRunnable implements Runnable {
		boolean active = true;
		SoundListener listener = null;
		SoundMonitorRunnable self = this;
		
		@Override
		public void run() {
			
			Util.runInSCThread(listener = new SoundListener());
			Util.runInSCThreadDelayed(new Runnable() {
				@Override
				public void run() {
					listener.active = false;
					listener = null;
					if (!active) return;
					
					Util.log("Background AMP: " + background_amp_level);
					Util.runInSCThreadDelayed(self, samplingInterval);
				}
			}, samplingLength);
		}
		
		class SoundListener implements Runnable {
			boolean active = true;
			@Override
			public void run() {
				if (!active) return;
				double amp = 0;
				if (recorder != null) {
					amp = recorder.getMaxAmplitude();
				}
				background_amp_level = background_amp_level * 0.7 + 0.3 * amp;
				Util.runInSCThreadDelayed(this, samplingRate);
			}
		}
	}
}
