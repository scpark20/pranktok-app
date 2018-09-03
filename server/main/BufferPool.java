package main;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferPool implements Unit {
	static private BufferPool instance = null;
	Queue<Buffer> bufferQueue;
	//ArrayList<Buffer> bufferList;
	int semaphores[];
	Object mutex = new Object();
	
	static public BufferPool getInstance()
	{
		if(instance==null)
			instance = new BufferPool();
		
		return instance;
	}
	
	private BufferPool()
	{
		semaphores = new int[C.BUFFER_ALLOC_SIZE];
		bufferQueue = new ConcurrentLinkedQueue<Buffer>();
		//bufferList = new ArrayList<Buffer>();
		for(int i=0;i<C.BUFFER_ALLOC_SIZE;i++)
		{
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(C.VOICE_DATA_SIZE);
			Buffer buffer = new Buffer(i, byteBuffer);
			bufferQueue.offer(buffer);
			//bufferList.add(buffer);
			semaphores[i] = 0;
		}
	}
	
	public Buffer allocate()
	{
		synchronized(mutex)
		{
			Buffer ret = null;
			ret = bufferQueue.poll();
			//LOG.I("BufferQueue " + bufferQueue.size());
			
			if(ret!=null)
			{
				ret.byteBuffer.clear();
				
				if(semaphores[ret.key]!=0)
				{
					//LOG.E("alloc fail " + ret.key + " " + semaphores[ret.key]);
				}
				semaphores[ret.key] = semaphores[ret.key]+1;
			}
			
			//LOG.I("buffer key : " + ret.key);
			return ret;
		}
	}
	
	public boolean deallocate(Buffer buffer)
	{
		synchronized(mutex)
		{
			if(buffer==null)
				return false;
			
			if(semaphores[buffer.key]<=0)
				return false;
			
			semaphores[buffer.key] = semaphores[buffer.key]-1;
			
			if(semaphores[buffer.key]!=0)
			{
				//LOG.E("dealloc fail");
			}
		
			bufferQueue.offer(buffer);
			
			return true;
		}
	}
	
	/*
	public void findUndeallocBuffer()
	{
		synchronized(mutex)
		{
			int count = bufferQueue.size();
			boolean[] checkList = new boolean[bufferList.size()];
			for(int i=0;i<count;i++)
			{
				Buffer buffer = bufferQueue.poll();
				if(bufferList.contains(buffer))
					checkList[buffer.key] = true;
				
				bufferQueue.offer(buffer);
				LOG.I(i + "");
			}
			
			for(int i=0;i<bufferList.size();i++)
			{
				if(!checkList[i])
					LOG.E(bufferList.get(i).getTags());
			}
		}
	}
	*/
	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		synchronized(mutex)
		{
			return "BufferPool size : " + this.bufferQueue.size() + "\n";
		}
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		synchronized(mutex)
		{
			return !this.bufferQueue.isEmpty();
		}
	}

	@Override
	public String getTag() {
		// TODO Auto-generated method stub
		return "BufferPool";
	}
}
