package voice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.BlockingQueue;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.MyThread;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.util.Log;
import network.SessionManager;
import session.Session;

public class AudioRecorder extends MyThread {
	static AudioRecorder instance = null;
	
	AudioRecord audioRecord;
	AcousticEchoCanceler AEC;
	int seqNumber = 0;
	int recordState;
	
	public static final int RECORDSTATE_RUNNING = 0;
	public static final int RECORDSTATE_STOPPED = 1;
	
	public static AudioRecorder getInstance()
	{
		if(instance==null)
			instance = new AudioRecorder();
		
		return instance;
	}

	public AudioRecorder()
	{
		
		
		int minSize = AudioRecord.getMinBufferSize(C.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
		
		audioRecord = new AudioRecord(AudioSource.VOICE_COMMUNICATION,
										C.SAMPLE_RATE,
										AudioFormat.CHANNEL_IN_MONO,
										AudioFormat.ENCODING_PCM_16BIT,
										minSize);
		
		AEC = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
		//AEC.setEnabled(true);
		
		recordState = RECORDSTATE_STOPPED;
		state = STATE_STOPPED;
		
	}
	
	public void setAECenable(boolean enable)
	{
		this.AEC.setEnabled(enable);
	}
	
	public void doStart()
	{
		if(state==STATE_STOPPED)
			this.start();
	}
	
	public void doStop()
	{
		if(state==STATE_RUNNING)
		{
			this.recordState = RECORDSTATE_STOPPED;
			this.state = STATE_STOPPED;
		}
	}
	
	
	@Override
	public void run()
	{
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{	
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
			}
			while(recordState==RECORDSTATE_RUNNING)
			{
				/*
				long currentTime = System.currentTimeMillis();
				gap = (gap * 0.9) + (currentTime - lastTime) * 0.1;
				Log.i("gap", "" + (int)gap);
				lastTime = currentTime;
				*/
				short[] shortArray = new short[C.VOICE_DATA_SIZE/2];
				audioRecord.read(shortArray, 0, shortArray.length);
				
				//Log.i("rms", getRMS(shortArray)+"");
				/*
				byte[] byteArray = new byte[C.VOICE_DATA_SIZE];
				
				
				for(int i=0;i<shortArray.length;i++)
					Conversion.shortToByteArray(shortArray[i], 0, byteArray, i*2, Short.SIZE/8);
				*/
				
				//int n = PhaseVocoder.getInstance().put(byteArray, byteArray);
				//Log.i("scpark", "VoiceRecorder " + (seqNumber++));
				VoiceEncoder.getInstance().put(shortArray);
				//Log.i("scpark", "AudioRecoder");
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		instance = null;
	}
	
	private double getRMS(short[] shortArray)
	{
		double rms = 0;
		for(int i=0;i<shortArray.length;i++)
			rms += shortArray[i] * shortArray[i];
		
		rms /= shortArray.length;
		rms = Math.sqrt(rms);
		
		return rms;
	}
	
	public void startRecord()
	{
		audioRecord.startRecording();
		this.recordState = RECORDSTATE_RUNNING;
	}
	
	public void stopRecord()
	{
		audioRecord.stop();
		this.recordState = RECORDSTATE_STOPPED;
	}
}
