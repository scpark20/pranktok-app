package voice;

public class VoicePriorityData implements Comparable<VoicePriorityData> {
	public int seqNumber;
	public byte[] voiceData;
	
	public VoicePriorityData(int seqNumber, byte[] voiceData)
	{
		this.seqNumber = seqNumber;
		this.voiceData = voiceData;
	}

	@Override
	public int compareTo(VoicePriorityData another) {
		// TODO Auto-generated method stub
		return this.seqNumber <= another.seqNumber ? -1 : 1;
	}
}
