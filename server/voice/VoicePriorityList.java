package voice;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;

public class VoicePriorityList {
	private LinkedList<Buffer> ownList;
	private int LIST_SIZE;
	private HashMap<Integer, Boolean> polledHashMap;
	
	public VoicePriorityList(int size)
	{
		this.ownList = new LinkedList<Buffer>();
		LIST_SIZE = size;
		polledHashMap = new HashMap<Integer, Boolean>();
	}
	
	public boolean offer(Buffer newBuffer)
	{
		int newSeqNumber = newBuffer.byteBuffer.getInt(C.SEQ_INDEX);
		if(polledHashMap.get(newSeqNumber)!=null)
			return false;
		
		int index = 0;
		for(;index<ownList.size();index++)
		{
			Buffer buffer = ownList.get(index);
			int seqNumber = buffer.byteBuffer.getInt(C.SEQ_INDEX);
			if(seqNumber==newSeqNumber)
				return false;
			else if(seqNumber<newSeqNumber)
				continue;
			else
				break;
		}
		
		polledHashMap.put(newSeqNumber, true);
		ownList.add(index, newBuffer);
		
		if(ownList.size()>LIST_SIZE)
			BufferPool.getInstance().deallocate(ownList.removeFirst());
		
		return true;
	}
	
	public Buffer poll()
	{
		
		if(ownList.isEmpty())
			return null;
		
		Buffer data = ownList.poll();
		
		return data;
	}
	
	public void reset()
	{
		Buffer buffer = ownList.poll();
		while(buffer!=null)
		{
			BufferPool.getInstance().deallocate(buffer);
			buffer = ownList.poll();
		}
		polledHashMap.clear();
	}
}
