package voice;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;

public class PacketTokenizer2 {
	Queue<Buffer> ownBufferQueue;
	ByteBuffer ownBuffer;
	static public long EOF_VALUE = 0x54F9A32B;
	
	public PacketTokenizer2()
	{
		ownBufferQueue = new LinkedList<Buffer>();
		ownBuffer = ByteBuffer.allocateDirect(65536);
	}
	
	
	public boolean put(ByteBuffer byteBuffer)
	{
		if(ownBuffer.remaining()<byteBuffer.remaining())
			ownBuffer.clear();
			
		ownBuffer.put(byteBuffer);
		
		int length = ownBuffer.getInt(0);
		if(length>ownBuffer.limit()-Long.SIZE/8 || length<0) // wrong length
		{
			discard(ownBuffer, true);
			return false;
		}
		else if(length>ownBuffer.position()-Long.SIZE/8) // possibility of not yet read whole packet
		{
			discard(ownBuffer, false);
			return true;
		}
		else
		{
			long expectedEOF = ownBuffer.getLong(length);
			if(expectedEOF==EOF_VALUE)
			{
				int savedPosition = ownBuffer.position();
				if(C.VOICE_DATA_SIZE>=length-Integer.SIZE/8)
				{
					Buffer newBuffer = BufferPool.getInstance().allocate();
					ownBuffer.position(Integer.SIZE/8);
					ownBuffer.limit(length);
					newBuffer.byteBuffer.put(ownBuffer);
					newBuffer.byteBuffer.flip();
					ownBufferQueue.offer(newBuffer);
				}
				
				ownBuffer.limit(savedPosition);
				ownBuffer.position(length+Long.SIZE/8);
				ownBuffer.compact();
				ownBuffer.position(savedPosition-length-Long.SIZE/8);
			}
			else
			{
				discard(ownBuffer, true);
				return false;
			}
		}
		
		return true;
	}
	
	private void discard(ByteBuffer byteBuffer, boolean clear)
	{
		int eofPosition = findEOF(byteBuffer, 0, byteBuffer.position());
		if(eofPosition<0)
		{
			if(clear)
				byteBuffer.clear();
		}
		else
		{
			int savedPosition = byteBuffer.position(); 
			byteBuffer.position(eofPosition+Long.SIZE/8);
			byteBuffer.compact();
			byteBuffer.position(savedPosition - eofPosition - Long.SIZE/8);
		}
	}
	
	private int findEOF(ByteBuffer byteBuffer, int start, int end)
	{
		for(int i=start;i<=end-Long.SIZE/8;i++)
		{
			long value = byteBuffer.getLong(i);
			if(value==EOF_VALUE)
				return i;
		}
		
		return -1;
	}
	
	public Buffer get()
	{
		return ownBufferQueue.poll();
	}
	
	public int getQueueSize()
	{
		return this.ownBufferQueue.size();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PacketTokenizer2 pt = new PacketTokenizer2();
		ByteBuffer[] inputBuffer = new ByteBuffer[100000];
		for(int i=0;i<inputBuffer.length;i++)
			inputBuffer[i] = ByteBuffer.allocate(2048);
		Random random = new Random();
		
		for(int i=0;i<inputBuffer.length;i++)
		{
			int length1 = 128;
			int length2 = 128;
			
			inputBuffer[i].putInt(length1+4);
			inputBuffer[i].put(new byte[length2]);
			inputBuffer[i].putLong(EOF_VALUE);
			inputBuffer[i].flip();
		}
		
		long time1 = System.currentTimeMillis();
		for(int i=0;i<inputBuffer.length;i++)
		{
			pt.put(inputBuffer[i]);
		}
		long time2 = System.currentTimeMillis();
		
		LOG.I("time " + (time2 - time1));			
	}

}
