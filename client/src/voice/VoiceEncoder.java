package voice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Queue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.MyThread;

import session.Session;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.util.Base64;
import android.util.Log;

public class VoiceEncoder extends MyThread {
	static private VoiceEncoder instance = null;
	public Queue<short[]> voiceDataQueue;
	int encodeState;
	
	public static final int ENCODESTATE_RUNNING = 0;
	public static final int ENCODESTATE_STOPPED = 1;
	
	
	static public VoiceEncoder getInstance()
	{
		if(instance == null)
			instance = new VoiceEncoder();
		
		return instance;
	}

	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		this.state=STATE_RUNNING;
		while(this.state==STATE_RUNNING)
		{
			//Log.i("scpark", "thread VoiceEncoder1");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
			}
			int seqNumber = 0;
			while(this.encodeState==ENCODESTATE_RUNNING)
			{
				//Log.i("scpark", "thread VoiceEncoder2");
				short[] shortArray = null;
				synchronized(voiceDataQueue)
				{
					shortArray = voiceDataQueue.poll();
					
				}
				
				if(shortArray==null)
				{
					continue;
				}
				
				
				PhaseVocoder.getInstance().put(shortArray);
				
				short[] outputShortArray = new short[8192];
				int n = PhaseVocoder.getInstance().get(outputShortArray);
				
				
				while(n>0)
				{
					short[] newOutputShortArray = new short[n];
				
					System.arraycopy(outputShortArray, 0, newOutputShortArray, 0, n);
				
					AudioPlayer.getInstance().put(newOutputShortArray);
				
					byte[] outputArray = new byte[16384];
					int encodedLength = put(newOutputShortArray, outputArray);
					
					if(encodedLength>0)
					{
						byte[] newArray = new byte[C.VOICE_DATA_SIZE];
						if(Session.getInstance().otherAESCrypto!=null)
						{
							int encryptedLength = 0;
							boolean encryptFail = false;
							try {
								encryptedLength = Session.getInstance().otherAESCrypto.encode(outputArray, encodedLength, newArray);
							} catch (ShortBufferException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								encryptFail = true;
							} catch (IllegalBlockSizeException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								encryptFail = true;
							} catch (BadPaddingException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								encryptFail = true;
							}
							if(!encryptFail && encryptedLength > 0)
							{
								byte[] encryptedArray = new byte[encryptedLength];
								System.arraycopy(newArray, 0, encryptedArray, 0, encryptedLength);
								
								VoicePriorityData newData = new VoicePriorityData(seqNumber, encryptedArray);
								Session.getInstance().putVoiceOutputData(newData);
								seqNumber++;	
							}
						}	
						
						
					}
			
					n = PhaseVocoder.getInstance().get(outputShortArray);
				}
			
			}
		}
		instance = null;
	}
	
		
	private VoiceEncoder()
	{
		init(C.SAMPLE_RATE, C.BIT_RATE);
		voiceDataQueue = new VoiceDataQueue<short[]>(C.VOICE_BUFFER_QUEUE_SIZE);
		
		this.state = STATE_STOPPED;
		encodeState = ENCODESTATE_STOPPED;
	}
	
	public void init(int sampleRate, int bitRate)
	{
		native_init(sampleRate, bitRate);
	}
	
	public int put(short[] inputArray, byte[] outputArray)
	{
		return native_put(inputArray, outputArray);
	}
	
	public void reset()
	{
		native_reset();
	}
	
	public void put(short[] shortArray)
	{
		synchronized(voiceDataQueue)
		{
			this.voiceDataQueue.offer(shortArray);
		}
	}
	
	public void doStart() {
		// TODO Auto-generated method stub
		if(this.state==STATE_STOPPED)
			this.start();
	}
	
	public void doStop() {
		if(this.state==STATE_RUNNING)
		{
			this.encodeState = ENCODESTATE_STOPPED;
			this.state = STATE_STOPPED;
		}
	}
	
	public void startEncoding()
	{
		
		encodeState = ENCODESTATE_RUNNING;
	}
	
	public void stopEncoding()
	{
		//codec.stop();
		reset();
		PhaseVocoder.getInstance().reset();
		encodeState = ENCODESTATE_STOPPED;
	}
	
	private native void native_init(int sampleRate, int bitRate);
	private native int native_put(short[] inputArray, byte[] outputArray);
	private native void native_reset();
	
	static{
		System.loadLibrary("prankcallclient");
	}
}
