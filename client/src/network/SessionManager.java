package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import com.scpark.prankcallclient.C;

import com.scpark.prankcallclient.MainActivity;
import com.scpark.prankcallclient.MyThread;
import com.scpark.prankcallclient.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import session.Session;
import session.SessionOps;

public class SessionManager extends MyThread {
	static SessionManager instance;
	
	SocketChannel socketChannel;
	Selector selector;
	PacketTokenizer packetTokenizer;
	
	ByteBuffer encodedBuffer;
	ByteBuffer tempBuffer;
	ByteBuffer byteBuffer;
	CharBuffer charBuffer;
	
	Charset charset;
	CharsetDecoder charsetDecoder;
	CharsetEncoder charsetEncoder;
	
	boolean CONNECTED;
		
	static public SessionManager getInstance() {
		// TODO Auto-generated method stub
		if(instance==null)
			instance = new SessionManager();
		
		return instance;
	}
	
	private SessionManager()
	{
		byteBuffer = ByteBuffer.allocate(C.SESSION_PACKET_SIZE);
		charBuffer = CharBuffer.allocate(C.SESSION_PACKET_SIZE/2);
		
		tempBuffer = ByteBuffer.allocate(C.SESSION_PACKET_SIZE);
		encodedBuffer = ByteBuffer.allocate(C.SESSION_PACKET_SIZE);
		
		
		charset = Charset.forName("UTF-8");
		charsetDecoder = charset.newDecoder();
		charsetEncoder = charset.newEncoder();
		
		packetTokenizer = new PacketTokenizer();
		state = STATE_STOPPED;
	}
	
	public void doStart()
	{
		if(state==STATE_STOPPED)
			this.start();
	}
	
	public void reconnect()
	{
		this.CONNECTED = false;
	}
	
	public void doStop()
	{
		if(state==STATE_RUNNING)
		{
			CONNECTED = false;
		}
	}
	
	@Override
	public void run()
	{
		state = STATE_RUNNING;
		boolean correctIP = true;
		while(Session.getInstance().state!=Session.STATE_ENDED)
		{
			CONNECTED = false;
			
			String serverIP = Session.getInstance().getServerIP();
			int serverPort = Session.getInstance().getServerPort();
			
			if(serverIP==Session.PREF_SERVER_IP_DEFAULT || serverPort==Session.PREF_SERVER_PORT_DEFAULT || !correctIP)
			{
				if(!getAddress())
				{
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					continue;
				}
			}
			
			try {
				init();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					
				}
				correctIP = false;
				continue;
			} catch (IllegalArgumentException e){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					
				}
				correctIP = false;
				continue;
			}
			
			while(MainActivity.getInstance()==null)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
				}
			
			Session.getInstance().OnNetworkConnected();
			
			while(CONNECTED)
			{
				
				//Log.i("scpark", "thread SessionManager");
				long currentTime = System.currentTimeMillis();
				if(currentTime > Session.getInstance().lastHeartBeatTime + C.HEART_BEAT_PERIOD && Session.getInstance().heartBeatSended == false)
				{
					Session.getInstance().queueHeartBeatOutput();
					Session.getInstance().heartBeatSended = true;
				}
				
				if(currentTime > Session.getInstance().lastHeartBeatTime + C.HEART_BEAT_TIME_OUT)
					CONNECTED = false;
				
				if(Session.getInstance().state == Session.STATE_ENDED && Session.getInstance().outputMessageQueue.size()==0)
				{
					int state = Session.getInstance().state;
					CONNECTED = false;
				}
				
				if(!CONNECTED)
					break;
				
				try {
					selector.select();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					CONNECTED = false;
				}
				
				
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while(iterator.hasNext() && CONNECTED)
				{
					SelectionKey selectionKey = iterator.next();
					
					if(selectionKey.isReadable())
					{
						SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
						try {
							read(socketChannel);
							
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							CONNECTED = false;
						}
					}
					
					if(selectionKey.isWritable())
						if(!Session.getInstance().outputMessageQueue.isEmpty())
						{
							try {
								write(socketChannel);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								CONNECTED = false;
							}
						}
					
					iterator.remove();
				}
			}
			//Log.i("scpark", "session close");
			close();
		}
		state = STATE_STOPPED;
		instance = null;
	}
	
	private void close() {
		// TODO Auto-generated method stub
		if(socketChannel!=null)
			close(socketChannel);
		try {
			if(selector!=null)
				selector.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	private void init() throws IOException
	{
		SocketAddress address = new InetSocketAddress(Session.getInstance().getServerIP(), Session.getInstance().getServerPort());
		
		//too long
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setReuseAddress(true);
		socketChannel.connect(address);
		
		
		for(int i=0;i<50;i++)
		{
			CONNECTED = socketChannel.finishConnect();
			if(CONNECTED)
				break;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		if(!CONNECTED)
		{
			socketChannel.close();
			throw new IOException();
		}
		
		selector = Selector.open();
		socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		
		Session.getInstance().lastHeartBeatTime = System.currentTimeMillis();
		Session.getInstance().heartBeatSended = false;
		
	}
	
	private void close(SocketChannel socketChannel)
	{
		try {
			socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	private int read(SocketChannel socketChannel) throws IOException 
	{
		byteBuffer.clear();
		
		
		socketChannel.read(byteBuffer);
		byteBuffer.flip();
		packetTokenizer.put(byteBuffer);
		ByteBuffer outByteBuffer = packetTokenizer.get();
	
		while(outByteBuffer!=null)
		{
			byte encryptedFlag = outByteBuffer.get();
			
			if(encryptedFlag!=C.UNENCRYPTED_FLAG)
			{
				Crypto crypto = null;
				if(encryptedFlag==C.RSA_ENCRYPTED_FLAG)
					crypto = Session.getInstance().myRSACrypto;
				else
					crypto = Session.getInstance().myAESCrypto;
				
				if(crypto==null)
					return -1;
				
			   try {
				   tempBuffer.clear();
				   crypto.decode(outByteBuffer, tempBuffer);
			   } catch (ShortBufferException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					return 0;
				} catch (IllegalBlockSizeException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					return 0;
				} catch (BadPaddingException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					return 0;
				}
			   tempBuffer.flip();
			   
			   outByteBuffer = tempBuffer;
			}
			
			charBuffer.clear();
			charsetDecoder.decode(outByteBuffer, charBuffer, true);
			charBuffer.flip();
			String inputString = charBuffer.toString();
			Session.getInstance().putInputMessage(inputString);
			outByteBuffer = packetTokenizer.get();
		}
		
		return 0;
	}
	
	private void write(SocketChannel socketChannel) throws IOException 
	{
		// TODO Auto-generated method stub
		
		String outputMessage = Session.getInstance().getOutputMessage();
		//Log.i("scpark", outputMessage);
		if(outputMessage != null)
		{
			byteBuffer.clear();
			Crypto crypto = null;
			if(outputMessage.startsWith(SessionOps.PUT_PUBLIC_KEY))
				byteBuffer.put(C.UNENCRYPTED_FLAG);
			else 
			{
				if(outputMessage.startsWith(SessionOps.PUT_SECRET_KEY))
				{
					crypto = Session.getInstance().serverRSACrypto;
					byteBuffer.put(C.RSA_ENCRYPTED_FLAG);
				}
				else
				{
					crypto = Session.getInstance().serverAESCrypto;
					byteBuffer.put(C.AES_ENCRYPTED_FLAG);
				}
				
				if(crypto==null)
					return;
			}
			
			CharBuffer charBuffer = CharBuffer.wrap(outputMessage);
			
			if(outputMessage.startsWith(SessionOps.PUT_PUBLIC_KEY))
				charsetEncoder.encode(charBuffer, byteBuffer, true);
			else
			{
				tempBuffer.clear();
				charsetEncoder.encode(charBuffer, tempBuffer, true);
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
			
			int n = 1;
			while(byteBuffer.hasRemaining() && n>0)
				n = socketChannel.write(byteBuffer);
		
			//Log.i("scpark", n+"");
		}
	}
	
	public boolean isConnected()
	{
		return CONNECTED;
	}
	
	public int getPort()
	{
		return this.socketChannel.socket().getPort();
	}
	
	private boolean getAddress()
	{
		String address = Address.getAddress();
		
		if(address!=null)
		{
			String[] token = address.split("/"); 
			Session.getInstance().putServerIP(Utils.longToIp(Long.parseLong(token[0])));
			Session.getInstance().putServerPort(Integer.parseInt(token[1]));
			return true;
		}
		else
			return false;
	}
}
