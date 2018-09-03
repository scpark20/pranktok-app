package voice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;
import main.MyThread;
import main.Unit;
import main.Utils;
import session.Session;
import session.SessionInfo;
import session.SessionManager;

public class VoiceUdpManager extends MyThread implements Unit {
	public int VOICE_PORT;
	
	DatagramChannel datagramChannel;
	Selector selector;
	
	long bytesPerSecond;
	long transferCheckLastTime;
	private HashMap<Session, Boolean> activeSessionMap;
	long lastLoopTime;
	
	public VoiceUdpManager(int port) throws IOException
	{
		this.VOICE_PORT = port;
		activeSessionMap = new HashMap<Session, Boolean>();
		init();
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		
		transferCheckLastTime = System.currentTimeMillis();
		bytesPerSecond = 0;
		
		LOG.I("voiceUdpManager port(" + VOICE_PORT + ") is started.");
		
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{
			long currentTime = System.currentTimeMillis();
			lastLoopTime = currentTime;
			
			if(currentTime > transferCheckLastTime + 1000)
			{
				
					//LOG.I(bytesPerSecond / 1024 + "kB/s");
					bytesPerSecond = 0;
				transferCheckLastTime = currentTime;
			}
				
			try {
				selector.select(1000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//LOG.E("SELECTOR IOException");
				continue;
			}
			
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			
			while(iterator.hasNext())
			{
				SelectionKey selectionKey = (SelectionKey) iterator.next();
				
				//read
				if(selectionKey.isReadable())
				{
					read((DatagramChannel) selectionKey.channel());
					
					
				}
				
				iterator.remove();
			}
			
		}
	}
	
	private void read(DatagramChannel datagramChannel) {
		// TODO Auto-generated method stub
		//LOG.I("UDP");
		Buffer buffer = BufferPool.getInstance().allocate();
		//buffer.addTag("VoiceUdpManager110");
		SocketAddress socketAddress = null;
		boolean readFail = false;
		try {
			socketAddress = datagramChannel.receive(buffer.byteBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			readFail = true;
		}
		
		if(socketAddress==null || readFail || buffer.byteBuffer.position()==0)
		{
			BufferPool.getInstance().deallocate(buffer);
			//LOG.I("read fail");
			
			return;
		}
		//LOG.I(((InetSocketAddress)socketAddress).getPort()+"");
		buffer.byteBuffer.flip();
		
		//check Length
		if(buffer.byteBuffer.limit()-C.LENGTH_TAG_SIZE<0 ||
				buffer.byteBuffer.limit()-C.LENGTH_TAG_SIZE!=buffer.byteBuffer.getInt(buffer.byteBuffer.limit()-C.LENGTH_TAG_SIZE))
		{
			BufferPool.getInstance().deallocate(buffer);
			//LOG.I("length error");
			return;
		}
		
		//get phoneNumber
		byte[] phoneNumberArray = new byte[SessionInfo.PHONE_NUMBER_LENGTH];
		buffer.byteBuffer.get(phoneNumberArray);
		
		String phoneNumber = null;
		try {
			phoneNumber = new String(phoneNumberArray, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(phoneNumber==null)
		{
			BufferPool.getInstance().deallocate(buffer);
			return;
		}
		
		Session session = null;
		try{
			session = SessionManager.getInstance().getSessionByPhoneNumber(phoneNumber);
		}catch(Exception e)
		{
			BufferPool.getInstance().deallocate(buffer);
			return;
		}
		
		if(session==null)
		{
			BufferPool.getInstance().deallocate(buffer);
			return;
		}
		
		if(this.activeSessionMap.get(session)==null)
		{
			//LOG.E("not active");
			BufferPool.getInstance().deallocate(buffer);
			return;
		}
		
		//get packetType
		byte packetType = buffer.byteBuffer.get();
		
		if(packetType==C.PACKET_TYPE_DATA)
		{
			//LOG.I("data");
			//get seq
			int seq = buffer.byteBuffer.getInt();
			
			buffer.byteBuffer.rewind();
			
			if(!session.putVoiceBuffer(buffer))
				BufferPool.getInstance().deallocate(buffer);
			
			Buffer getBuffer = session.getVoiceBuffer();
			
			while(getBuffer!=null)
			{
				try {
					
					datagramChannel.send(getBuffer.byteBuffer, socketAddress);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				BufferPool.getInstance().deallocate(getBuffer);
				getBuffer = session.getVoiceBuffer();
			}
		}
		else if(packetType==C.PACKET_TYPE_IP)
		{
			//LOG.I("ip");
			long privateIP = buffer.byteBuffer.getLong();
			int privatePort = buffer.byteBuffer.getInt();
			
			InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
			InetAddress inetAddress = inetSocketAddress.getAddress();
			long publicIP = Utils.ipToLong(inetAddress.getHostAddress());
			int publicPort = inetSocketAddress.getPort();
			
			session.putUdpIPs(privateIP, privatePort, publicIP, publicPort);
			BufferPool.getInstance().deallocate(buffer);
		}
	}

	
	private void init() throws IOException
	{
		selector = Selector.open();
		datagramChannel = DatagramChannel.open();
		datagramChannel.socket().bind(new InetSocketAddress(VOICE_PORT));
		datagramChannel.configureBlocking(false);
		datagramChannel.register(selector, SelectionKey.OP_READ);
	}
	
	public void close()
	{
		try {
			selector.close();
			datagramChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateTransferAmount(int position) {
		// TODO Auto-generated method stub
			bytesPerSecond += position;
	
	}
	
	public synchronized void registerSession(Session session)
	{
		synchronized(activeSessionMap)
		{
			activeSessionMap.put(session, true);
		}
	}
	
	public synchronized void unregisterSession(Session session)
	{
		synchronized(activeSessionMap)
		{
			this.activeSessionMap.remove(session);
		}
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder(1024);
		sb.append("VoiceUdpManager [");
		sb.append(VOICE_PORT);
		sb.append("] - ");
		
		synchronized(activeSessionMap)
		{
			sb.append("activeSessionMap : ");
			sb.append(activeSessionMap.size());
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
		return "VoiceUdpManager" + this.VOICE_PORT;
	}
}
