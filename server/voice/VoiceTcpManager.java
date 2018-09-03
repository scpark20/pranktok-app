package voice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;
import main.MyThread;
import main.R;
import main.Unit;
import main.Utils;
import session.ConnectionMonitor;
import session.Session;
import session.SessionInfo;
import session.SessionManager;

public class VoiceTcpManager extends MyThread implements Unit {
	public int VOICE_PORT;
	
	private static VoiceUdpManager instance = null;
	ServerSocketChannel serverSocketChannel;
	Selector selector;
	ArrayList<SocketChannel> socketChannelList;
	HashMap<SocketChannel, Long> lastTransmissionTimeMap;
	Random random;
	private HashMap<Session, Boolean> activeSessionMap;
	public ConcurrentHashMap<SocketChannel, Integer> errorCountMap;
	
	Object key = new Object();
	
	long lastLoopTime;
	
	public VoiceTcpManager(int port) throws IOException
	{
		this.VOICE_PORT = port;
		socketChannelList = new ArrayList<SocketChannel>();
		lastTransmissionTimeMap = new HashMap<SocketChannel, Long>();
		random = new Random();
		activeSessionMap = new HashMap<Session, Boolean>();
		errorCountMap = new ConcurrentHashMap<SocketChannel, Integer>();
		init();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		
		LOG.I("voiceTcpManager port(" + VOICE_PORT + ") is started.");
		
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{
			
			long currentTime = System.currentTimeMillis();
			lastLoopTime = currentTime;
			
			if(!socketChannelList.isEmpty())
			{
				int index = random.nextInt(socketChannelList.size());
				SocketChannel randomSocketChannel = socketChannelList.get(index);
				long lastTransmissionTime = this.lastTransmissionTimeMap.get(randomSocketChannel);
				if(lastTransmissionTime + C.VOICE_TCP_TRANSMISSION_TIME_OUT < currentTime)
					close(randomSocketChannel);
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
				
				if(selectionKey.isAcceptable())
					try {
						accept(selectionKey);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				
				//read
				if(selectionKey.isReadable())
				{
					SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
					int n = 0;
					
					n = read(socketChannel);
					
					if(n<0)
						this.addErrorCount(socketChannel, n);
				}
				
				
				iterator.remove();
			}
			
		}
	}
	
	private int read(SocketChannel socketChannel)
	{
		//LOG.I("TCP");
		SocketAddress socketAddress = null;
		boolean readFail = false;
		try {
			socketAddress = socketChannel.getRemoteAddress();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			readFail = true;
		}
		
		int n = 0;
		Buffer buffer = BufferPool.getInstance().allocate();
		//buffer.addTag("VoiceTcpManager146");
		try {
			n = socketChannel.read(buffer.byteBuffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			readFail = true;
		}
		
		if(socketAddress==null || readFail || n<=0)
		{
			BufferPool.getInstance().deallocate(buffer);
			return R.ERROR;
		}
		
		this.lastTransmissionTimeMap.put(socketChannel, System.currentTimeMillis());
		
		buffer.byteBuffer.flip();
		
		PacketTokenizer packetTokenizer = null;
		if(socketChannel!=null)
		{
			packetTokenizer = SessionManager.getInstance().tokenizerMapByVoiceSocketChannel.get(socketChannel);
			if(packetTokenizer == null)
			{
				//LOG.E("packetTokenizer can't find.");
				BufferPool.getInstance().deallocate(buffer);
				return R.SEVERE;
			}
			
			if(!packetTokenizer.put(buffer.byteBuffer))
			{
				BufferPool.getInstance().deallocate(buffer);
				return R.ERROR;
			}
			
			BufferPool.getInstance().deallocate(buffer);
			buffer = packetTokenizer.get();
		}
		else
		{
			BufferPool.getInstance().deallocate(buffer);
			return R.SEVERE;
		}
		
		while(buffer!=null)
		{
		
			//check Length
			if(buffer.byteBuffer.limit()-C.LENGTH_TAG_SIZE!=buffer.byteBuffer.getInt(buffer.byteBuffer.limit()-C.LENGTH_TAG_SIZE))
			{
				BufferPool.getInstance().deallocate(buffer);
				//LOG.I("length problem");
				return R.WARNING;
			}
			
			buffer.byteBuffer.position(0);
			
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
				return R.SEVERE;
			}
			
			Session session = SessionManager.getInstance().getSessionByPhoneNumber(phoneNumber);
			
			if(session==null)
			{
				BufferPool.getInstance().deallocate(buffer);
				return R.SEVERE;
			}
			
			if(this.activeSessionMap.get(session)==null)
			{
				BufferPool.getInstance().deallocate(buffer);
				return R.SEVERE;
			}
			
			//get packetType
			byte packetType = buffer.byteBuffer.get();
			
			if(packetType==C.PACKET_TYPE_DATA)
			{
				//get seq
				int seq = buffer.byteBuffer.getInt();
				
				buffer.byteBuffer.rewind();
				
				if(!session.putVoiceBuffer(buffer))
					BufferPool.getInstance().deallocate(buffer);
				
				Buffer getBuffer = session.getVoiceBuffer();
				
				int count1 = 0;
				while(getBuffer!=null && count1<10)
				{
					if(getBuffer.byteBuffer.limit()+Long.SIZE/8 < getBuffer.byteBuffer.capacity())
					{
						int savedLimit = getBuffer.byteBuffer.limit();
						getBuffer.byteBuffer.limit(savedLimit+Long.SIZE/8);
						getBuffer.byteBuffer.putLong(savedLimit, PacketTokenizer.EOF_VALUE);
						
						int count2 = 0;
						while(getBuffer.byteBuffer.hasRemaining() && count2<10)
						{
							try {
								socketChannel.write(getBuffer.byteBuffer);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								break;
							}
							count2++;
						}
					}
					
					BufferPool.getInstance().deallocate(getBuffer);
					getBuffer = session.getVoiceBuffer();
					
					count1++;
				}	
				
				BufferPool.getInstance().deallocate(getBuffer);
			}
			else if(packetType==C.PACKET_TYPE_IP)
			{
				long privateIP = buffer.byteBuffer.getLong();
				int privatePort = buffer.byteBuffer.getInt();
				
				InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
				InetAddress inetAddress = inetSocketAddress.getAddress();
				long publicIP = Utils.ipToLong(inetAddress.getHostAddress());
				int publicPort = inetSocketAddress.getPort();
				
				session.putTcpIPs(privateIP, privatePort, publicIP, publicPort);
				
				BufferPool.getInstance().deallocate(buffer);
			}
			else
				BufferPool.getInstance().deallocate(buffer);
			
			buffer = packetTokenizer.get();
		}
		return R.GOOD;
	}
	
	private void close(SocketChannel socketChannel) {
		// TODO Auto-generated method stub
		/*
		try {
			LOG.I("close voice TCP " + socketChannel.getRemoteAddress().toString());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/
		SessionManager.getInstance().tokenizerMapByVoiceSocketChannel.get(socketChannel).flush();
		SessionManager.getInstance().tokenizerMapByVoiceSocketChannel.remove(socketChannel);
		socketChannelList.remove(socketChannel);
		this.errorCountMap.remove(socketChannel);
		this.lastTransmissionTimeMap.remove(socketChannel);
		try {
			socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

	private void init() throws IOException
	{
		selector = Selector.open();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(VOICE_PORT));
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	public void close()
	{
		try {
			selector.close();
			serverSocketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	private void accept(SelectionKey selectionKey) throws IOException
	{
		boolean acceptDeny = false;
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		InetSocketAddress socketAddress = null;
		try {
			socketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			 acceptDeny = true;
		}
		
		String ipString = "";
		
		if(acceptDeny==false)
		{
			ipString = (socketAddress.getAddress()).getHostAddress();
			long ipLong = Utils.ipToLong(ipString);
			
			if(!ConnectionMonitor.getInstance().isValidIP(ipLong))
				acceptDeny = true;
		}
		
		if(acceptDeny==true)
		{
			socketChannel.close();
			return;
		}
		
		socketChannel.configureBlocking(false);
		socketChannelList.add(socketChannel);
		lastTransmissionTimeMap.put(socketChannel, System.currentTimeMillis());
		socketChannel.register(selector, SelectionKey.OP_READ);
		
		SessionManager.getInstance().tokenizerMapByVoiceSocketChannel.put(socketChannel, new PacketTokenizer());
	}
	
	public void registerSession(Session session)
	{
		synchronized(activeSessionMap)
		{
			this.activeSessionMap.put(session, true);
		}
	}
	
	public void unregisterSession(Session session)
	{
		synchronized(activeSessionMap)
		{
			this.activeSessionMap.remove(session);
		}
	}
	
	public void addErrorCount(SocketChannel socketChannel, int addCount)
	{
		Integer errorCount = errorCountMap.get(socketChannel);
		if(errorCount==null)
			errorCount = addCount;
		else
			errorCount += addCount;
		
		if(errorCount < C.ERROR_COUNT_MAXIMUM)
		{
			this.close(socketChannel);
			return;
		}
		errorCountMap.put(socketChannel, errorCount);
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder(1024);
		sb.append("VoiceTcpManager [");
		sb.append(VOICE_PORT);
		sb.append("] - ");
		
		synchronized(socketChannelList)
		{
			sb.append("socketChannelList : ");
			sb.append(socketChannelList.size());
		}
		
		synchronized(lastTransmissionTimeMap)
		{
			sb.append(" lastTransmissionTimeMap : ");
			sb.append(lastTransmissionTimeMap.size());
		}
		synchronized(activeSessionMap)
		{
			sb.append(" activeSessionMap : ");
			sb.append(activeSessionMap.size());
		}
		synchronized(errorCountMap)
		{
			sb.append(" errorCountMap : ");
			sb.append(errorCountMap.size());
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
		return "VoiceTcpManager" + this.VOICE_PORT;
	}
	
}
