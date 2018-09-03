package voice;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;

public class VoiceDataList {
	private LinkedList<Buffer> ownList;
	private int LIST_SIZE;
	private HashMap<Integer, Boolean> offeredHashMap;
	
	public VoiceDataList(int size)
	{
		this.ownList = new LinkedList<Buffer>();
		LIST_SIZE = size;
		offeredHashMap = new HashMap<Integer, Boolean>();
	}
	
	public boolean offer(Buffer newBuffer)
	{
		synchronized(ownList)
		{
			int newSeqNumber = newBuffer.byteBuffer.getInt(C.SEQ_INDEX);
			if(offeredHashMap.get(newSeqNumber)!=null)
				return false;
			
			offeredHashMap.put(newSeqNumber, true);
			ownList.add(newBuffer);
			
			if(ownList.size()>LIST_SIZE)
				BufferPool.getInstance().deallocate(ownList.removeFirst());
		}	
		return true;
	}
	
	public Buffer poll()
	{
		Buffer data = null;
		synchronized(ownList)
		{
			if(ownList.isEmpty())
				return null;
			
			data = ownList.poll();
		}
		return data;
	}
	
	public void reset()
	{
		synchronized(ownList)
		{
			Buffer buffer = ownList.poll();
			while(buffer!=null)
			{
				BufferPool.getInstance().deallocate(buffer);
				buffer = ownList.poll();
			}
			offeredHashMap.clear();
		}
	}
	
	public int size()
	{
		synchronized(ownList)
		{
			return ownList.size();
		}
	}
}
