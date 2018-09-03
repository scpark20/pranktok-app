package voice;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;

public class PacketTokenizer {
	Queue<Buffer> ownBufferQueue;
	ByteBuffer ownBuffer;
	static public long EOF_VALUE = 0x54F9A32B;
	int prevPosition;
	
	public PacketTokenizer()
	{
		ownBufferQueue = new LinkedList<Buffer>();
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
						
						Buffer newBuffer = BufferPool.getInstance().allocate();
						//newBuffer.addTag("PacketTokenizer60");
						if(newBuffer.byteBuffer.remaining()<ownBuffer.remaining())
							ownBuffer.limit(ownBuffer.position()+newBuffer.byteBuffer.remaining());
						
						newBuffer.byteBuffer.put(ownBuffer);
						newBuffer.byteBuffer.flip();
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
	
	public Buffer get()
	{
		return ownBufferQueue.poll();
	}
	
	public void flush() {
		// TODO Auto-generated method stub
		prevPosition = 0;
		ownBuffer.clear();
		Buffer buffer = ownBufferQueue.poll();
		while(buffer!=null)
		{
			BufferPool.getInstance().deallocate(buffer);
			buffer = ownBufferQueue.poll();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PacketTokenizer pt = new PacketTokenizer();
		ByteBuffer[] inputBuffer = new ByteBuffer[100000];
		for(int i=0;i<inputBuffer.length;i++)
			inputBuffer[i] = ByteBuffer.allocate(2048);
		Random random = new Random();
		
		for(int i=0;i<inputBuffer.length;i++)
		{
			int length2 = 128;
			
			inputBuffer[i].put(new byte[length2+random.nextInt(10)]);
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


