package voice;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import android.util.Base64;
import android.util.Log;

import com.scpark.prankcallclient.C;

import session.Session;

public class OpenSLAudioPlayer implements IAudioPlayer {
	static private OpenSLAudioPlayer instance = null;
	int state;
	public static int STATE_INITED = 0;
	public static int STATE_STARTED = 1;
	public static int STATE_UNINITED = 3;
	
	static public OpenSLAudioPlayer getInstance()
	{
		if(instance==null)
			instance = new OpenSLAudioPlayer();
		
		return instance;
	}
	
	private OpenSLAudioPlayer()
	{
		state = STATE_UNINITED;
	}
	
	public void uninit()
	{
		if(this.state == STATE_INITED)
		{
			this.native_uninit();
			this.state = STATE_UNINITED;
		}
	}

	@Override
	public void init(int systemSampleRate, int sampleRate, int bufferSize) {
		// TODO Auto-generated method stub
		if(this.state == STATE_UNINITED)
		{
			native_init(systemSampleRate, sampleRate, bufferSize);
			this.state = STATE_INITED;
		}
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		if(this.state==STATE_INITED)
		{
			native_start();
			this.state = STATE_STARTED;
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		if(this.state == STATE_STARTED)
		{
			native_stop();
			state = STATE_INITED;
		}
	}

	@Override
	synchronized public void put(int seqNumber, byte[] byteArray) {
		// TODO Auto-generated method stub
		//Log.i("scpark", "decoded length" + byteArray.length);
		if(Session.getInstance().myAESVoiceCrypto==null)
			return;
		
		byte[] newArray = new byte[C.VOICE_DATA_SIZE];
		int n = 0;
		boolean decodeFail = false;
		try {
			n = Session.getInstance().myAESVoiceCrypto.decode(byteArray, byteArray.length, newArray);
		} catch (ShortBufferException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			decodeFail = true;
			
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			decodeFail = true;
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			decodeFail = true;
		}
		
		if(decodeFail || n<=0)
			return;
		
		byte[] decodedArray = new byte[n];
		System.arraycopy(newArray, 0, decodedArray, 0, n);
		
		native_put(seqNumber, decodedArray);
	}

	private native void native_init(int systemSampleRate, int sampleRate, int bufferSize);
	private native void native_uninit();
	private native void native_start();
	private native void native_stop();
	private native void native_put(int seqNumber, byte[] byteArray);
	
	static{
		System.loadLibrary("prankcallclient");
	}

}
