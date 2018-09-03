package voice;

public interface IAudioPlayer {
	public void init(int systemSampleRate, int sampleRate, int bufferSize);
	public void start();
	public void stop();
	public void put(int seqNumber, byte[] byteArray);
	
}
