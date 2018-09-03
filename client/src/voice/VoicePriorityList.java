package voice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import android.util.Log;

public class VoicePriorityList {
	private LinkedList<VoicePriorityData> ownList;
	private int LIST_SIZE;
	private HashMap<Integer, Boolean> polledHashMap;
	
	public VoicePriorityList(int size)
	{
		this.ownList = new LinkedList<VoicePriorityData>();
		LIST_SIZE = size;
		polledHashMap = new HashMap<Integer, Boolean>();
	}
	
	public boolean offer(VoicePriorityData newData)
	{
		if(polledHashMap.get(newData.seqNumber)!=null)
			return false;
		
		int index = 0;
		for(;index<ownList.size();index++)
		{
			VoicePriorityData data = ownList.get(index);
			if(data.seqNumber==newData.seqNumber)
				return false;
			else if(data.seqNumber<newData.seqNumber)
				continue;
			else
				break;
		}
		
		polledHashMap.put(newData.seqNumber, true);
		ownList.add(index, newData);
		
		if(ownList.size()>LIST_SIZE)
			ownList.removeFirst();
		
		return true;
	}
	
	public VoicePriorityData poll()
	{
		if(ownList.isEmpty())
			return null;
		
		VoicePriorityData data = ownList.removeFirst();
		
		return data;
	}
	
	public void reset()
	{
		ownList.clear();
		polledHashMap.clear();
	}
}
