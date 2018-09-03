package session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import main.Buffer;
import main.BufferPool;
import main.C;
import main.LOG;
import main.Unit;
import main.Utils;
import voice.PacketTokenizer;
import voice.VoiceDataList;
import voice.VoicePriorityList;
import voice.VoiceTcpManager;
import voice.VoiceUdpManager;

public class Session implements Unit {
	int sessionID;
	SocketChannel socketChannel;
	long ipLong = -1;
	PacketTokenizer packetTokenizer;
	
	ConcurrentLinkedQueue<String> outputMessageQueue;
	VoiceDataList voiceBufferQueue;
	SessionInfo sessionInfo;
	public long lastHeartBeatTime;
	Random random;
	RSACrypto clientRSACrypto;
	AESCrypto clientAESCrypto;
	private AESCrypto sessionAESCrypto;
	
	public SocketAddress udpSocketAddress;
	public SocketChannel tcpSocketChannel;
	private ConcurrentLinkedQueue<Buffer> inputBufferQueue;
	
	private Session callingSession = null;
	private SessionHandler sessionHandler = null;
	
	private ConcurrentLinkedQueue<VoiceTcpManager> voiceTcpManagerQueue;
	private ConcurrentLinkedQueue<VoiceUdpManager> voiceUdpManagerQueue;
	
	public Integer state;
	long connectTime;
	
	boolean ADMIN = false;
	
	static public final int STATE_INITED = 0;
	static public final int STATE_STARTED = 1;
	static public final int STATE_ENDED = 2;
	static public final int STATE_CALLING = 3;
	
	public Session(int sessionID) {
		// TODO Auto-generated constructor stub
		this.sessionID = sessionID;
		outputMessageQueue = new ConcurrentLinkedQueue<String>();
		voiceBufferQueue = new VoiceDataList(C.VOICE_BUFFER_QUEUE_SIZE);
		this.sessionInfo = new SessionInfo();
		callingSession = null;
		random = new Random();
		packetTokenizer = new PacketTokenizer();
		inputBufferQueue = new ConcurrentLinkedQueue<Buffer>();
		voiceTcpManagerQueue = new ConcurrentLinkedQueue<VoiceTcpManager>(); 
		voiceUdpManagerQueue = new ConcurrentLinkedQueue<VoiceUdpManager>();
		
		state = STATE_ENDED;
	}
	
	public void init(SocketChannel socketChannel, SessionHandler sessionHandler)
	{
		connectTime = System.currentTimeMillis();
		this.socketChannel = socketChannel;
		this.sessionHandler = sessionHandler;
		InetSocketAddress socketAddress = null;
		try {
			socketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(socketAddress != null)
		{
			InetAddress inetAddress = socketAddress.getAddress();
			String ipString = inetAddress.getHostAddress();
			ipLong = Utils.ipToLong(inetAddress.getHostAddress());
			ConnectionMonitor.getInstance().putConnectedIP(ipLong, this);
			ConnectionMonitor.getInstance().registerErrorCount(this);
		}
		
		//LOG.I(socketChannel.socket().getInetAddress() + " " + socketChannel.socket().getPort() + " has been connected.");
		lastHeartBeatTime = System.currentTimeMillis();
		//datagramChannelList = new CopyOnWriteArrayList<DatagramChannel>();
		udpSocketAddress = null;
		
		sessionAESCrypto = new AESCrypto();
		
		ADMIN = false;
		state = STATE_INITED;
	}

	public void putInputMessage(String inputMessage) 
	{
		// TODO Auto-generated method stub
		//LOG.I("Input Message : " + inputMessage);
		
		String[] operationArray = inputMessage.split(C.OPERATION_DELIMITER_STRING);
		
		if(operationArray.length==0)
			return;
		
		for(String operation: operationArray)
			putOperation(operation);
	}
	
	void putOperation(String operation)
	{
		String[] recordArray = operation.split(C.RECORD_DELIMITER_STRING);
		
		if(recordArray.length==0)
			return;
		
		String opString = recordArray[0];
		
		if(opString.equals(SessionOps.BYE))
		{
			if(recordArray.length!=1)
				return;
			
			SessionManager.getInstance().closeSession(this);
		}
		
		else if(opString.equals(SessionOps.ADMIN))
		{
			if(recordArray.length!=2)
				return;
			
			String passwd = recordArray[1];
			if(passwd.equals(C.ADMIN_PASSWORD))
				ADMIN = true;
		}
		
		else if(opString.equals(SessionOps.ADMIN_GET_STATUS))
		{
			if(!ADMIN)
				return;
			
			if(recordArray.length!=1)
				return;
			
			LinkedList<String> runningList = ConnectionMonitor.getInstance().getCheckRunningString();
			
			String operationString = buildOperation(SessionOps.ADMIN_PUT_STATUS_START, null);
			registerOutputMessage(operationString);
			
			String args1[] = {"userCount " + SessionManager.getInstance().getUserCount() +"\n"};
			operationString = buildOperation(SessionOps.ADMIN_PUT_STATUS, args1);
			registerOutputMessage(operationString);
			
			for(String string:runningList)
			{
				String args[] = {string};
				operationString = buildOperation(SessionOps.ADMIN_PUT_STATUS, args);
				registerOutputMessage(operationString);
			}
			
			operationString = buildOperation(SessionOps.ADMIN_PUT_STATUS_END, null);
			registerOutputMessage(operationString);
		}
		
		else if(opString.equals(SessionOps.PUT_PUBLIC_KEY))
		{
			if(recordArray.length!=2)
				return;
			
			this.clientRSACrypto = new RSACrypto(recordArray[1]);
			
			String args[] = {sessionHandler.handlerRSAcrypto.getPublicKeyString()};
			String operationString = buildOperation(SessionOps.PUT_PUBLIC_KEY, args);
			registerOutputMessage(operationString);
		}
		
		else if(opString.equals(SessionOps.PUT_SECRET_KEY))
		{
			if(recordArray.length!=2)
				return;
			
			this.clientAESCrypto = new AESCrypto(recordArray[1]);
			
			
			String args[] = {sessionAESCrypto.getSecretKeyString()};
			String operationString = buildOperation(SessionOps.PUT_SECRET_KEY, args);
			registerOutputMessage(operationString);
		}
		
		else if(opString.equals(SessionOps.PUT_CURRENT_VERSION))
		{
			if(recordArray.length!=2)
				return;
			
			int version = Integer.parseInt(recordArray[1]);
			
			if(version!=C.CURRENT_VERSION)
			{
				String operationString = buildOperation(SessionOps.VERSION_ERROR, null);
				registerOutputMessage(operationString);
				ConnectionMonitor.getInstance().queueInvalidSession(this);
			}
			else
			{
				String operationString = buildOperation(SessionOps.VERSION_OK, null);
				registerOutputMessage(operationString);
			}
		}
		
		else if(opString.equals(SessionOps.GET_NEWPRANKKEY))
		{
			long newPrankKey = SessionManager.getInstance().makeNewPrankKey();
			
			if(newPrankKey==0)
				return;
			
			String args[] = { newPrankKey + ""};
			String operationString = buildOperation(SessionOps.PUT_PRANKKEY, args);
			registerOutputMessage(operationString);
			
			this.sessionInfo.prankKey = newPrankKey;
			SessionManager.getInstance().sessionMapByPrankKey.put(sessionInfo.prankKey, this);
			
			//new phonenumber
			String newPhoneNumber = SessionManager.getInstance().allocNewPhoneNumber(sessionInfo.prankKey);
			if(newPhoneNumber==null)
				return;
			
			SessionManager.getInstance().sessionMapByPhoneNumber.remove(Utils.getPhoneNumberHashCode(sessionInfo.phoneNumber));			
			this.sessionInfo.phoneNumber = newPhoneNumber;
			SessionManager.getInstance().sessionMapByPhoneNumber.put(Utils.getPhoneNumberHashCode(sessionInfo.phoneNumber), this);
			
			String[] args1 = {sessionInfo.phoneNumber};
			operationString = buildOperation(SessionOps.PUT_PHONENUMBER, args1);
			registerOutputMessage(operationString);
			
			state = STATE_STARTED;
		}
		
		else if(opString.equals(SessionOps.PUT_PRANKKEY))
		{
			if(recordArray.length<2)
				return;
			
			long prankKey = Long.parseLong(recordArray[1]);
			
			this.sessionInfo.prankKey = prankKey;
			String getedPhoneNumber = SessionManager.getInstance().getPhoneNumberByPrankKey(prankKey);
			
			if(getedPhoneNumber!=null)
			{
				if(getedPhoneNumber.equals("null"))
				{
					String newPhoneNumber = SessionManager.getInstance().allocNewPhoneNumber(sessionInfo.prankKey);
					if(newPhoneNumber==null)
						return;
					
					this.sessionInfo.phoneNumber = newPhoneNumber;
					
					long hashCode = Utils.getPhoneNumberHashCode(sessionInfo.phoneNumber);
					SessionManager.getInstance().sessionMapByPhoneNumber.put(hashCode, this);
					
					String args[] = {sessionInfo.phoneNumber};
					String operationString = buildOperation(SessionOps.PUT_PHONENUMBER, args);
					registerOutputMessage(operationString);
					
					state = STATE_STARTED;
				}
				else
				{
					this.sessionInfo.phoneNumber = getedPhoneNumber;
					long hashedPhoneNumber = Utils.getPhoneNumberHashCode(sessionInfo.phoneNumber);
					
					SessionManager.getInstance().sessionMapByPhoneNumber.put(hashedPhoneNumber, this);
					Session puttedSession = SessionManager.getInstance().sessionMapByPhoneNumber.get(hashedPhoneNumber);
					
					/*
					if(puttedSession!=null)
						LOG.I("putted " + hashedPhoneNumber);
					else
						LOG.I("putted error");
					*/
					
					String args[] = {sessionInfo.phoneNumber};
					String operationString = buildOperation(SessionOps.PUT_PHONENUMBER, args);
					registerOutputMessage(operationString);
					
					Session session = this;
					Session existSession = SessionManager.getInstance().sessionMapByPrankKey.get(sessionInfo.prankKey);
					if(existSession!=null)
						ConnectionMonitor.getInstance().queueInvalidSession(existSession);
					
					SessionManager.getInstance().sessionMapByPrankKey.put(sessionInfo.prankKey, this);
					
					state = STATE_STARTED;
				}
			}
			
			else
			{
				String operationString = buildOperation(SessionOps.INVALID_PRANKKEY, null);
				registerOutputMessage(operationString);
			}
		}
		
		else if(opString.equals(SessionOps.HEART_BEAT))
		{
			//LOG.I("Operation : " + operation);
			this.lastHeartBeatTime = System.currentTimeMillis();
			String args[] = {};
			String operationString = buildOperation(SessionOps.HEART_BEAT, args);
			registerOutputMessage(operationString);
		}
		
		
		else if(opString.equals(SessionOps.GET_NEWNICKNAME))
		{
			//LOG.I("Operation : " + operation);
			String newNickname = SessionManager.getInstance().makeNickname(this.sessionInfo.country);
			sessionInfo.nickName = newNickname;
			
			String args[] = {sessionInfo.nickName};
			String operationString = buildOperation(SessionOps.PUT_NICKNAME, args);
			registerOutputMessage(operationString);
		}
		
		
		else if(opString.equals(SessionOps.GET_NEWPHONENUMBER))
		{
			//LOG.I("Operation : " + operation);
			
			if(this.sessionInfo.prankKey==0)
				return;
			
			String newPhoneNumber = SessionManager.getInstance().allocNewPhoneNumber(sessionInfo.prankKey);
			if(newPhoneNumber==null)
				return;
			
			SessionManager.getInstance().sessionMapByPhoneNumber.remove(Utils.getPhoneNumberHashCode(sessionInfo.phoneNumber));			
			this.sessionInfo.phoneNumber = newPhoneNumber;
			SessionManager.getInstance().sessionMapByPhoneNumber.put(Utils.getPhoneNumberHashCode(sessionInfo.phoneNumber), this);
			
			String args[] = {sessionInfo.phoneNumber};
			String operationString = buildOperation(SessionOps.PUT_PHONENUMBER, args);
			registerOutputMessage(operationString);
			
			state = STATE_STARTED;
		}
		
		else if(opString.equals(SessionOps.PUT_NICKNAME))
		{
			if(recordArray.length<2)
				return;
			
			String newNickName = recordArray[1];
			this.sessionInfo.nickName = newNickName;
		}
		
		else if(opString.equals(SessionOps.PUT_EMOJI))
		{
			if(recordArray.length!=2)
				return;
			
			int newEmoji = Integer.parseInt(recordArray[1]);
			this.sessionInfo.emoji = newEmoji;
		}
		
		else if(opString.equals(SessionOps.PUT_COUNTRY))
		{
			if(recordArray.length!=2)
				return;
			
			this.sessionInfo.country = recordArray[1];
		}
		
		else if(opString.equals(SessionOps.GET_USERLIST))
		{
			//LOG.I("Operation : " + operation);
			if(recordArray.length!=2)
				return;
			
			boolean myCountryOnly = Integer.parseInt(recordArray[1])==1?true:false;
			
			String operationString = buildOperation(SessionOps.PUT_USERLISTSTART, null);
			registerOutputMessage(operationString);
			
			ArrayList<Session> sessionList = SessionManager.getInstance().getUserList(this, C.USER_LIST_COUNT, myCountryOnly?sessionInfo.country:null);
			if(sessionList==null)
				return;
			
			for(Session session: sessionList)
			{
				operationString = session.getUserListData();
				registerOutputMessage(operationString);
			}
			operationString = buildOperation(SessionOps.PUT_USERLISTEND, null);
			registerOutputMessage(operationString);
		}
		
		else if(opString.equals(SessionOps.GET_USERINFO))
		{
			if(recordArray.length!=2)
				return;
			
			String phoneNumber = recordArray[1];
			String[] userData = SessionManager.getInstance().getUserData(phoneNumber);
			
			if(userData==null)
			{
				String[] args = {phoneNumber};
				String operationString = buildOperation(SessionOps.PUT_USERINFO, args);
				registerOutputMessage(operationString);
			}
			else
			{
				String operationString = buildOperation(SessionOps.PUT_USERINFO, userData);
				registerOutputMessage(operationString);
			}
		}
		
		
		
		else if(opString.equals(SessionOps.CALL_TO))
		{
			if(recordArray.length!=2)
				return;
			
			String phoneNumber = recordArray[1];
			Session dstSession = SessionManager.getInstance().sessionMapByPhoneNumber.get(Utils.getPhoneNumberHashCode(phoneNumber));
			
			if(dstSession==null || dstSession==this)
			{
				String operationString = buildOperation(SessionOps.WRONG_NUMBER_ERROR, null);
				registerOutputMessage(operationString);
			}
			else if(dstSession.state!=Session.STATE_STARTED)
			{
				String operationString = buildOperation(SessionOps.DEST_CALLING_ERROR, null);
				registerOutputMessage(operationString);
			}
			else
			{
				String[] args = {dstSession.sessionInfo.nickName, dstSession.sessionInfo.phoneNumber, dstSession.sessionInfo.emoji+"", dstSession.sessionInfo.country, dstSession.clientRSACrypto.getPublicKeyString()};
				String operationString = buildOperation(SessionOps.CALL_RESPONSE, args);
				registerOutputMessage(operationString);
				
				this.callingSession = dstSession;
			}
		}
		
		else if(opString.equals(SessionOps.CALL_TO2))
		{
			if(recordArray.length!=2)
				return;
			
			if(callingSession==null)
			{
				String operationString = buildOperation(SessionOps.WRONG_NUMBER_ERROR, null);
				registerOutputMessage(operationString);
				callingSession = null;
				return;
			}
			else if(callingSession.state!=Session.STATE_STARTED)
			{
				String operationString = buildOperation(SessionOps.DEST_CALLING_ERROR, null);
				registerOutputMessage(operationString);
				callingSession = null;
				return;
			}
			
			String secretKey = recordArray[1];
			//LOG.C("CALL_TO2 " + secretKey);
			callingSession.calledFrom(this, this.clientRSACrypto.getPublicKeyString(), secretKey);
			state = STATE_CALLING;
		}
		
		else if(opString.equals(SessionOps.CALL_ACCEPT))
		{
			if(recordArray.length!=2)
				return;
			
			if(state!=STATE_CALLING)
				return;
			
			voiceBufferQueue.reset();
			
			String secretKey = recordArray[1];
			
			VoiceTcpManager voiceTcpManager = ConnectionMonitor.getInstance().getActiveVoiceTcpManager();
			
			if(voiceTcpManager!=null)
			{
				voiceTcpManager.registerSession(this);
				this.voiceTcpManagerQueue.offer(voiceTcpManager);
				
				String[] args = new String[]{C.SERVER_IP+"", voiceTcpManager.VOICE_PORT+""};
				String operationString = buildOperation(SessionOps.PUT_TCP_SERVER_IP, args);
				registerOutputMessage(operationString);
			}
			
			VoiceUdpManager[] voiceUdpManagers = ConnectionMonitor.getInstance().getActiveVoiceUdpManagers();
			if(voiceUdpManagers[0]!=null)
			{
				for(VoiceUdpManager voiceUdpManager: voiceUdpManagers)
				{
					voiceUdpManager.registerSession(this);
					this.voiceUdpManagerQueue.offer(voiceUdpManager);
				}
				
				String[] args = new String[]{C.SERVER_IP+"", voiceUdpManagers[0].VOICE_PORT+"", C.VOICE_UDP_PORT_OFFER_COUNT+""};
				String operationString = buildOperation(SessionOps.PUT_UDP_SERVER_IP, args);
				registerOutputMessage(operationString);
			}
			
			if(callingSession==null)
			{
				String operationString = buildOperation(SessionOps.CALL_ACCEPT_ERROR, null);
				registerOutputMessage(operationString);
			}
			else
				callingSession.callAccept(secretKey);
			
			this.state = STATE_CALLING;
		}
		
		else if(opString.equals(SessionOps.ENABLE_FROM))
		{
			if(recordArray.length!=8)
				return;
			
			
			/*
			LOG.I(recordArray[0] + recordArray[1] + " " + recordArray[2] + " " + recordArray[3] + " " + recordArray[4] + " "
							+ recordArray[5] + " " + recordArray[6] + " " + recordArray[7]);
			*/
			
			String[] fromArray = {recordArray[1], recordArray[2], recordArray[3], recordArray[4], recordArray[5], recordArray[6], recordArray[7]};
			if(callingSession!=null)
				callingSession.requestEnableFrom(fromArray);
			
		}
		
		
		else if(opString.equals(SessionOps.CALL_OFF))
		{
			if(state != STATE_CALLING)
				return;
			
			if(recordArray.length!=1)
				return;
			
			if(callingSession!=null)
				callingSession.callOff();
			
			this.callingSession = null;
			
			//this.datagramChannelList.clear();
			this.udpSocketAddress = null;
			this.tcpSocketChannel = null;
			
			VoiceTcpManager tcpManager = voiceTcpManagerQueue.poll(); 
			while(tcpManager!=null)
			{
				tcpManager.unregisterSession(this);
				tcpManager = voiceTcpManagerQueue.poll();
			}
			
			VoiceUdpManager udpManager = voiceUdpManagerQueue.poll(); 
			while(udpManager!=null)
			{
				udpManager.unregisterSession(this);
				udpManager = voiceUdpManagerQueue.poll();
			}
			
			this.voiceBufferQueue.reset();
			
			state = STATE_STARTED;
		}	
		
	}

	private String buildOperation(String op, String[] args)
	{
		StringBuilder sb = new StringBuilder(C.SESSION_PACKET_SIZE);
		sb.append(op);
		sb.appendCodePoint(C.RECORD_DELIMITER);
		
		if(args!=null)
			for(String arg: args)
			{
				sb.append(arg);
				sb.appendCodePoint(C.RECORD_DELIMITER);
			}
			
		sb.appendCodePoint(C.OPERATION_DELIMITER);
		return sb.toString();
	}
	
	
	public String getOutputMessage()
	{
		String outputMessage = this.outputMessageQueue.poll(); 
		//LOG.I("Output Message : " + outputMessage);
		return outputMessage;
	}
	
	
	private void registerOutputMessage(String outputMessage)
	{
		outputMessageQueue.offer(outputMessage);
		sessionHandler.queueSession(this);
	}
	
	public String[] getUserData()
	{
		String args[] = {sessionInfo.nickName, sessionInfo.phoneNumber, sessionInfo.emoji+"", sessionInfo.country};
		return args;
	}

	public String getUserListData()
	{
		String args[] = {sessionInfo.nickName, sessionInfo.phoneNumber, sessionInfo.emoji+"", sessionInfo.country};
		String operationString = buildOperation(SessionOps.PUT_USERLIST, args);
		
		return operationString;
	}

	public void calledFrom(Session session, String publicKey, String secretKey) {
		// TODO Auto-generated method stub
		
		this.callingSession = session;
		String args[] = {session.sessionInfo.nickName, session.sessionInfo.phoneNumber, session.sessionInfo.emoji+"", session.sessionInfo.country, publicKey, secretKey};
		
		//LOG.C("calledFrom " + secretKey);
		String operationString = buildOperation(SessionOps.CALLED_FROM, args);
		registerOutputMessage(operationString);
		
		state = STATE_CALLING;
	}

	public void callAccept(String secretKey) {
		// TODO Auto-generated method stub
		
		String args[] = {secretKey};
		String operationString = buildOperation(SessionOps.CALL_ACCEPT, args);
		registerOutputMessage(operationString);
		
		VoiceTcpManager voiceTcpManager = ConnectionMonitor.getInstance().getActiveVoiceTcpManager();
		
		if(voiceTcpManager!=null)
		{
			voiceTcpManager.registerSession(this);
			this.voiceTcpManagerQueue.offer(voiceTcpManager);
			
			args = new String[]{C.SERVER_IP+"", voiceTcpManager.VOICE_PORT+""};
			operationString = buildOperation(SessionOps.PUT_TCP_SERVER_IP, args);
			registerOutputMessage(operationString);
		}
		
		VoiceUdpManager[] voiceUdpManagers = ConnectionMonitor.getInstance().getActiveVoiceUdpManagers();
		if(voiceUdpManagers[0]!=null)
		{
			for(VoiceUdpManager voiceUdpManager: voiceUdpManagers)
			{
				voiceUdpManager.registerSession(this);
				this.voiceUdpManagerQueue.offer(voiceUdpManager);
			}
			
			args = new String[]{C.SERVER_IP+"", voiceUdpManagers[0].VOICE_PORT+"", C.VOICE_UDP_PORT_OFFER_COUNT+""};
			operationString = buildOperation(SessionOps.PUT_UDP_SERVER_IP, args);
			registerOutputMessage(operationString);
		}
		
		voiceBufferQueue.reset();
	}
	
	private void callOff() {
		// TODO Auto-generated method stub
		String operationString = buildOperation(SessionOps.CALL_OFF, null);
		registerOutputMessage(operationString);
		
		this.udpSocketAddress = null;
		this.tcpSocketChannel = null;
		
		//this.datagramChannelList.clear();
		
		VoiceTcpManager tcpManager = voiceTcpManagerQueue.poll(); 
		while(tcpManager!=null)
		{
			tcpManager.unregisterSession(this);
			tcpManager = voiceTcpManagerQueue.poll();
		}
		
		VoiceUdpManager udpManager = voiceUdpManagerQueue.poll(); 
		while(udpManager!=null)
		{
			udpManager.unregisterSession(this);
			udpManager = voiceUdpManagerQueue.poll();
		}
		
		this.callingSession = null;
		this.voiceBufferQueue.reset();
		
		state = STATE_STARTED;
		
		
	}

	static Session tempSession = null;
	
	public boolean putVoiceBuffer(Buffer buffer) {
		// TODO Auto-generated method stub
		synchronized(voiceBufferQueue)
		{
			return this.voiceBufferQueue.offer(buffer);
		}
	}
	
	
	public Buffer getVoiceOwnBuffer() {
		// TODO Auto-generated method stub
		synchronized(voiceBufferQueue)
		{
			return this.voiceBufferQueue.poll();
		}
	}
	
	public Buffer getVoiceBuffer() {
		// TODO Auto-generated method stub
		if(callingSession!=null)
		{
			return callingSession.getVoiceOwnBuffer();
		}
		else
			return null;
	}
	
	public void OnClose() {
		// TODO Auto-generated method stub
		this.state = STATE_ENDED;
		
		packetTokenizer.flush();
		voiceBufferQueue.reset();
		
		synchronized(inputBufferQueue)
		{
			Buffer buffer = inputBufferQueue.poll();
			while(buffer!=null)
			{
				BufferPool.getInstance().deallocate(buffer);
				buffer = inputBufferQueue.poll();
			}
		}
		
		
		if(callingSession!=null)
		{
			callingSession.callOff();
			callingSession = null;
		}
		
		SessionManager.getInstance().sessionMapByPrankKey.remove(this.sessionInfo.prankKey);
		
		if(ipLong!=-1)
			ConnectionMonitor.getInstance().removeConnectedIP(ipLong, this);
		ipLong = -1;
		
		ConnectionMonitor.getInstance().removeSessionInErrorCountMap(this);
		
		this.tcpSocketChannel = null;
		this.udpSocketAddress = null;
		//this.datagramChannelList.clear();
		
		VoiceTcpManager tcpManager = voiceTcpManagerQueue.poll(); 
		while(tcpManager!=null)
		{
			tcpManager.unregisterSession(this);
			tcpManager = voiceTcpManagerQueue.poll();
		}
		
		VoiceUdpManager udpManager = voiceUdpManagerQueue.poll(); 
		while(udpManager!=null)
		{
			udpManager.unregisterSession(this);
			udpManager = voiceUdpManagerQueue.poll();
		}
	}
	
	private void requestEnableFrom(String[] fromArray) {
		// TODO Auto-generated method stub
		
		String args[] = fromArray;
		String operationString = buildOperation(SessionOps.ENABLE_FROM, args);
		registerOutputMessage(operationString);
	}

	public void putUdpIPs(long privateIP, int privatePort, long publicIP, int publicPort) {
		// TODO Auto-generated method stub
		
		if(callingSession!=null)
			callingSession.offerUdpIPs(privateIP, privatePort, publicIP, publicPort);
		
	}
	
	public void putTcpIPs(long privateIP, int privatePort, long publicIP, int publicPort) {
		// TODO Auto-generated method stub
		
		if(callingSession!=null)
			callingSession.offerTcpIPs(privateIP, privatePort, publicIP, publicPort);
		
	}

	private void offerUdpIPs(long privateIP, int privatePort, long publicIP, int publicPort) {
		// TODO Auto-generated method stub
		String args[] = {privateIP+"", privatePort+"", publicIP+"", publicPort+""};
		String operationString = buildOperation(SessionOps.PUT_UDP_IPs, args);
		registerOutputMessage(operationString);
		
		//LOG.I("offerUdpIPs " + Utils.longToIp(privateIP) + " " + privatePort + " " + Utils.longToIp(publicIP) + " " + publicPort);
	}
	
	private void offerTcpIPs(long privateIP, int privatePort, long publicIP, int publicPort) {
		// TODO Auto-generated method stub
		String args[] = {privateIP+"", privatePort+"", publicIP+"", publicPort+""};
		String operationString = buildOperation(SessionOps.PUT_TCP_IPs, args);
		registerOutputMessage(operationString);
		
		//LOG.I("offerTcpIPs " + Utils.longToIp(privateIP) + " " + privatePort + " " + Utils.longToIp(publicIP) + " " + publicPort);
	}
	
	
	public void addSocketAddress(SocketAddress socketAddress)
	{
		this.udpSocketAddress = socketAddress;
	}
	
	public void addTcpSocketChannel(SocketChannel socketChannel)
	{
		this.tcpSocketChannel = socketChannel;
	}
	
	/*
	public void addDatagramChannel(DatagramChannel datagramChannel)
	{
		if(this.datagramChannelList.contains(datagramChannel))
			return;
		
		this.datagramChannelList.add(datagramChannel);
	}
	*/
	public SocketAddress getOthersSocketAddress() {
		// TODO Auto-generated method stub
		if(this.callingSession==null)
			return null;
		
		return this.callingSession.udpSocketAddress;
	}
	
	/*
	public DatagramChannel getOthersDatagramChannel() {
		if(this.callingSession==null)
			return null;
		
		if(this.callingSession.datagramChannelList.isEmpty())
			return null;
		
		int index = random.nextInt(this.callingSession.datagramChannelList.size());
		
		return this.callingSession.datagramChannelList.get(index);
	}
	*/
	public SocketChannel getOthersSocketChannel() {
		// TODO Auto-generated method stub
		if(this.callingSession==null)
			return null;
		
		return this.callingSession.tcpSocketChannel;
	}

	public boolean isReadable() {
		// TODO Auto-generated method stub
		synchronized(inputBufferQueue)
		{
			if(!inputBufferQueue.isEmpty())
				return true;
			else 
				return false;
		}
	}

	public boolean isWritable() {
		// TODO Auto-generated method stub
		synchronized(outputMessageQueue)
		{
			if(outputMessageQueue.isEmpty())
				return false;
			else
				return true;
		}
	}

	public Buffer getInputBuffer() {
		// TODO Auto-generated method stub
		synchronized(inputBufferQueue)
		{
			return inputBufferQueue.poll();
		}
	}

	public void putInputBuffer(Buffer buffer) {
		// TODO Auto-generated method stub
		synchronized(inputBufferQueue)
		{
			inputBufferQueue.offer(buffer);
		}
	}
	
	public AESCrypto getSessionAESCrypto()
	{
		return this.sessionAESCrypto;
		
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder(2048);
		sb.append("Session[");
		sb.append(sessionID);
		sb.append("] - ");
		
		synchronized(outputMessageQueue)
		{
			sb.append("outputMessageQueue : ");
			sb.append(outputMessageQueue.size());
		}
		
		synchronized(voiceBufferQueue)
		{
			sb.append(" voiceBufferQueue : ");
			sb.append(voiceBufferQueue.size());
		}
		
		synchronized(inputBufferQueue)
		{
			sb.append(" inputBufferQueue : ");
			sb.append(inputBufferQueue.size());
		}
		
		synchronized(voiceTcpManagerQueue)
		{
			sb.append(" voiceTcpManagerQueue : ");
			sb.append(voiceTcpManagerQueue.size());
		}
		
		synchronized(voiceUdpManagerQueue)
		{
			sb.append(" voiceUdpManagerQueue : ");
			sb.append(voiceUdpManagerQueue.size());
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public String getTag() {
		// TODO Auto-generated method stub
		return "Session" + this.sessionID;
	}
}

