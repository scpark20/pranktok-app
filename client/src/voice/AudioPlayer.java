package voice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.MyThread;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaFormat;
import android.util.Log;
import network.SessionManager;
import session.Session;

public class AudioPlayer extends MyThread {
	static AudioPlayer instance = null;
	public Queue<short[]> voiceDataQueue;
	
	AudioTrack audioTrack;
	short[] shortArray;
	
	int playState;
	
	public static final int PLAYSTATE_RUNNING = 0;
	public static final int PLAYSTATE_STOPPED = 1;
	
	public static AudioPlayer getInstance()
	{
		if(instance==null)
			instance = new AudioPlayer();
		
		return instance;
	}
	
	public AudioPlayer()
	{

		shortArray = new short[16384];
		
		int minSize = AudioTrack.getMinBufferSize(C.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
		
		audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 
							C.SAMPLE_RATE, 
							AudioFormat.CHANNEL_OUT_MONO,
							AudioFormat.ENCODING_PCM_16BIT,
							minSize,
							AudioTrack.MODE_STREAM);
		
		voiceDataQueue = new VoiceDataQueue<short[]>(C.VOICE_BUFFER_QUEUE_SIZE);
		
		this.playState = PLAYSTATE_STOPPED;
		state = STATE_STOPPED;
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
			this.playState = PLAYSTATE_STOPPED;
			this.state = STATE_STOPPED;
		}
	}

	static long lastTime = 0;
	static double gap = 0;
	@Override
	public void run()
	{
		state = STATE_RUNNING;
		
		
		while(state==STATE_RUNNING)
		{
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			
			while(playState==PLAYSTATE_RUNNING)
			{
				short[] shortArray = null;
				synchronized(voiceDataQueue)
				{
					shortArray = voiceDataQueue.poll();
				}
				
				if(shortArray==null)
				{	
					Thread.yield();
					break;
				}
				
				//Log.i("rms", getRMS(shortArray)+"");
				audioTrack.write(shortArray, 0, shortArray.length);
				
				/*
				long currentTime = System.currentTimeMillis();
				gap = (gap * 0.9) + (currentTime - lastTime) * 0.1;
				Log.i("gap", "" + (int)gap);
				lastTime = currentTime;
				*/
				
			}
		}
		instance = null;
	}
	
	public void put(short[] shortArray)
	{
		synchronized(this.voiceDataQueue)
		{
			this.voiceDataQueue.offer(shortArray);
		}
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
	
	public void startPlay()
	{
		voiceDataQueue.clear();
		audioTrack.play();
		this.playState = PLAYSTATE_RUNNING;
	}
	
	public void stopPlay()
	{
		audioTrack.stop();
		this.playState = PLAYSTATE_STOPPED;
	}
}
