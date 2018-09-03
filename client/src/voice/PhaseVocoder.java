package voice;

import com.scpark.prankcallclient.C;

public class PhaseVocoder {
	static private PhaseVocoder instance = null;

	
	static public PhaseVocoder getInstance()
	{
		if(instance==null)
			instance = new PhaseVocoder(C.SAMPLE_RATE, C.VOICE_DATA_SIZE, 8, 4);
		
		return instance;
	}
	
	private PhaseVocoder(int sampleRate, int sampleLength, int FFTLogSize, int overlapRatio)
	{
		native_init(sampleRate, sampleLength, FFTLogSize, overlapRatio);
	}
	
	public int put(short[] inputArray, short[] outputArray)
	{
		return native_put(inputArray, outputArray);
	}
	
	public void put(short[] shortArray) {
		// TODO Auto-generated method stub
		native_put(shortArray);
	}
	
	public int get(short[] shortArray) {
		// TODO Auto-generated method stub
		return native_get(shortArray);
	}
	
	public void reset()
	{
		native_reset();
	}
	
	public void setPitchrate(double pitchRate)
	{
		native_setPitchRate(pitchRate);
	}
	
	private native void native_init(int sampleRate, int sampleLength, int FFTLogSize, int overlapRatio);
	private native int native_put(short[] inputArray,short[] outputArray);
	private native int native_put(short[] inputArray);
	private native int native_get(short[] outputArray);
	private native void native_setPitchRate(double pitchRate);
	private native void native_reset();
	
	static{
		System.loadLibrary("prankcallclient");
	}

	
}
