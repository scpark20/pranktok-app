package network;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import com.scpark.prankcallclient.C;

public class PacketTokenizer {
	Queue<ByteBuffer> ownBufferQueue;
	ByteBuffer ownBuffer;
	static public long EOF_VALUE = 0x54F9A32B;
	int prevPosition;
	
	public PacketTokenizer()
	{
		ownBufferQueue = new LinkedList<ByteBuffer>();
		ownBuffer = ByteBuffer.allocateDirect(16384);
		prevPosition = 0;
	}
	
	public boolean put(ByteBuffer byteBuffer)
	{
		int savedLimit = byteBuffer.limit();
		
		while(byteBuffer.remaining()>0)
		{
			if(!ownBuffer.hasRemaining())
			{
				//LOG.E("can't find EOF -> tokenizer flush");
				this.ownBuffer.clear();
				return false;
			}
			
			byteBuffer.limit(savedLimit);
			
			if(byteBuffer.remaining()>ownBuffer.remaining())
				byteBuffer.limit(byteBuffer.position()+ownBuffer.remaining());
		
			ownBuffer.put(byteBuffer);
			
			int i=prevPosition;
			int end=ownBuffer.position();
			while(i<=end-Long.SIZE/8)
			{
				long value = ownBuffer.getLong(i);
				if(value==EOF_VALUE)
				{
					ownBuffer.position(0);
					ownBuffer.limit(i);
					
					int savedLimit2 = ownBuffer.limit();
					while(ownBuffer.hasRemaining())
					{
						ownBuffer.limit(savedLimit2);
						
						ByteBuffer newBuffer = ByteBuffer.allocate(com.scpark.prankcallclient.C.VOICE_DATA_SIZE);	
						if(newBuffer.remaining()<ownBuffer.remaining())
							ownBuffer.limit(ownBuffer.position()+newBuffer.remaining());
						
						newBuffer.put(ownBuffer);
						newBuffer.flip();
						ownBufferQueue.offer(newBuffer);
					}
						
					ownBuffer.limit(end);
					ownBuffer.position(i+Long.SIZE/8);
					
					int copyLength = ownBuffer.limit() - ownBuffer.position(); 
					
					ownBuffer.compact();
					
					i = 0;
					end = copyLength;
					ownBuffer.position(end);
				}
				else
					i++;
			}
			
			prevPosition = i;
		}	
		
		return true;
	}
	
	public ByteBuffer get()
	{
		return ownBufferQueue.poll();
	}
	
	public void flush() {
		// TODO Auto-generated method stub
		prevPosition = 0;
		ownBuffer.clear();
		ownBufferQueue.clear();
	}
}


