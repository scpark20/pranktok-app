package session;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;
import main.MyThread;
import main.R;
import main.Unit;
import voice.PacketTokenizer;

public class SessionHandler extends MyThread implements Unit {
	public ConcurrentLinkedQueue<Session> sessionQueue;
	private int handlerIndex;
	
	ByteBuffer byteBuffer;
	byte[] byteArray;
	ByteBuffer tempBuffer;
	ByteBuffer encodedBuffer;
	//CharBuffer charBuffer;
	
	//Charset charset;
	//CharsetDecoder charsetDecoder;
	//CharsetEncoder charsetEncoder;
	RSACrypto handlerRSAcrypto = new RSACrypto();
	long lastLoopTime;
	
	public SessionHandler(int index)
	{
		this.handlerIndex = index;
		sessionQueue = new ConcurrentLinkedQueue<Session>();
		byteBuffer = ByteBuffer.allocate(C.SESSION_PACKET_SIZE);
		byteArray = new byte[C.SESSION_PACKET_SIZE];
		tempBuffer = ByteBuffer.allocate(C.SESSION_PACKET_SIZE);
		encodedBuffer = ByteBuffer.allocate(C.SESSION_PACKET_SIZE);
		
		//charBuffer = CharBuffer.allocate(C.SESSION_PACKET_SIZE/2);
		
		//charset = Charset.forName("UTF-8");
		//charsetDecoder = charset.newDecoder();
		//charsetEncoder = charset.newEncoder();
	}
	
	@Override
	public void run()
	{
		this.state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{
			lastLoopTime = System.currentTimeMillis();
			
			Session session = null;
			synchronized(sessionQueue)
			{
				session = sessionQueue.peek();
				if(session == null)
					try {
						sessionQueue.wait(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			session = sessionQueue.poll();
			
			if(session==null)
				continue;
			
			if(session.isReadable())
			{	
				int n = read(session);
				if(n<0)
					ConnectionMonitor.getInstance().addErrorCount(session, n);
			}
			
			if(session.isWritable())
				write(session);
		}
	}

	private void write(Session session) {
		// TODO Auto-generated method stub
		String outputMessage = session.getOutputMessage();
		SocketChannel socketChannel = session.socketChannel;
		
		byteBuffer.clear();
		
		//LOG.I(outputMessage);
		Crypto crypto = null;
		if(outputMessage.startsWith(SessionOps.PUT_PUBLIC_KEY))
			byteBuffer.put(C.UNENCRYPTED_FLAG);
		else 
		{
			if(outputMessage.startsWith(SessionOps.PUT_SECRET_KEY))
			{
				crypto = session.clientRSACrypto;
				byteBuffer.put(C.RSA_ENCRYPTED_FLAG);
			}
			else
			{
				crypto = session.clientAESCrypto;
				byteBuffer.put(C.AES_ENCRYPTED_FLAG);
			}
			
			if(crypto==null)
				return;
		}
			
		//CharBuffer writeCharBuffer = CharBuffer.wrap(outputMessage);
		
		if(outputMessage.startsWith(SessionOps.PUT_PUBLIC_KEY))
		{
			//charsetEncoder.encode(writeCharBuffer, byteBuffer, true);
			try {
				byteBuffer.put(outputMessage.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			tempBuffer.clear();
			//charsetEncoder.encode(writeCharBuffer, tempBuffer, true);
			try {
				tempBuffer.put(outputMessage.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			tempBuffer.flip();
			
			encodedBuffer.clear();
				try {
					crypto.encode(tempBuffer, encodedBuffer);
				} catch (ShortBufferException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				} catch (IllegalBlockSizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				} catch (BadPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			
			encodedBuffer.flip();
			byteBuffer.put(encodedBuffer);
		}
		
		byteBuffer.putLong(PacketTokenizer.EOF_VALUE);
		byteBuffer.flip();
		
		try {
			while(byteBuffer.hasRemaining())
				socketChannel.write(byteBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			ConnectionMonitor.getInstance().queueInvalidSession(session);
		}
	}

	private int read(Session session) {
		// TODO Auto-generated method stub
		Buffer buffer = session.getInputBuffer();
		
		if(buffer==null)
			return R.ERROR;
		
		ByteBuffer byteBuffer = buffer.byteBuffer;
		
		if(!session.packetTokenizer.put(byteBuffer))
		{
			BufferPool.getInstance().deallocate(buffer);
			return R.SEVERE;
		}
		
		BufferPool.getInstance().deallocate(buffer);
			
		Buffer outBuffer = session.packetTokenizer.get();
		if(outBuffer==null)
			return R.GOOD;
		
		ByteBuffer finalBuffer = outBuffer.byteBuffer;
		while(outBuffer!=null)
		{
			byte encryptedFlag = outBuffer.byteBuffer.get();
			
			if(encryptedFlag!=C.UNENCRYPTED_FLAG)
			{
				Crypto crypto = null;
				if(encryptedFlag==C.RSA_ENCRYPTED_FLAG)
					crypto = handlerRSAcrypto;
				else
					crypto = session.getSessionAESCrypto();
					
				if(crypto==null)
					return R.SEVERE;
				
				tempBuffer.clear();
					try {
						crypto.decode(outBuffer.byteBuffer, tempBuffer);
					} catch (ShortBufferException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						BufferPool.getInstance().deallocate(outBuffer);
						return R.SEVERE;
					} catch (IllegalBlockSizeException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						BufferPool.getInstance().deallocate(outBuffer);
						//LOG.E(session.sessionInfo.prankKey + " crypto fail");
						return R.SEVERE;
					} catch (BadPaddingException e) {
						// TODO Auto-generated catch block
						BufferPool.getInstance().deallocate(outBuffer);
						//LOG.E(session.sessionInfo.prankKey + " crypto fail");
						return R.SEVERE;
					}
			
				tempBuffer.flip();
				finalBuffer = tempBuffer;
			}
			
			/*
			charBuffer.clear();
			charsetDecoder.decode(finalBuffer, charBuffer, true);
			charBuffer.flip();
			session.putInputMessage(charBuffer.toString());
			*/
			
			finalBuffer.get(byteArray, 0, finalBuffer.remaining());
			
			try {
				session.putInputMessage(new String(byteArray, 0, finalBuffer.limit(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				
			}
			BufferPool.getInstance().deallocate(outBuffer);
			outBuffer = session.packetTokenizer.get();
		}	
		
		return R.GOOD;
	}
	
	public void queueSession(Session session)
	{
		synchronized(sessionQueue)
		{
			sessionQueue.offer(session);
			sessionQueue.notify();
		}
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder(1024);
		sb.append("SessionHandler [");
		sb.append(handlerIndex);
		sb.append("] - ");
		
		synchronized(sessionQueue)
		{
			sb.append("sessionQueue : ");
			sb.append(sessionQueue.size());
		}
		sb.append("\n");
		return sb.toString();
	}
	
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return this.lastLoopTime + C.THREAD_ALIVE_TIME > System.currentTimeMillis();
	}
	
	@Override
	public String getTag() {
		// TODO Auto-generated method stub
		return "SessionHandler" + this.handlerIndex;
	}
}

