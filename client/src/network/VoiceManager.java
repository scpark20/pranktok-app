package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.MyThread;
import com.scpark.prankcallclient.Utils;

import android.util.Log;
import session.Session;
import session.SessionInfo;
import voice.OpenSLAudioPlayer;
import voice.VoicePriorityData;
import voice.VoicePriorityList;

public class VoiceManager extends MyThread {
	int type;
	TransmissionChannel transmissionChannel;
	VoicePriorityList voiceOutputDataQueue;
	PacketTokenizer packetTokenizer;
	
	Selector selector;
	Random random;
	
	byte fromFlag;
	
	Charset charset;
	CharsetDecoder charsetDecoder;
	CharsetEncoder charsetEncoder;
	
	int transmissionState;
	boolean connected;
	
	int sendIPCount;
	
	static final int TRANSMISSIONSTATE_STARTED = 0;
	static final int TRANSMISSIONSTATE_STOPPED = 1;
	
	public VoiceManager(int type, byte fromFlag)
	{
		this.type = type;
		charset = Charset.forName("UTF-8");
		charsetDecoder = charset.newDecoder();
		charsetEncoder = charset.newEncoder();
		random = new Random();
		this.fromFlag = fromFlag;
		voiceOutputDataQueue = new VoicePriorityList(C.VOICE_BUFFER_QUEUE_SIZE);
		packetTokenizer = new PacketTokenizer();
		transmissionState = TRANSMISSIONSTATE_STOPPED;
	}
	
	public boolean isConnected()
	{
		return connected;
	}
	
	public boolean isTransmissionStarted()
	{
		return this.transmissionState == TRANSMISSIONSTATE_STARTED;
	}
	
	public void startTransmission()
	{
		sendIPCount = 0;
		this.transmissionState = TRANSMISSIONSTATE_STARTED;
	}
	
	public void stopTransmission()
	{
		this.transmissionState = TRANSMISSIONSTATE_STOPPED;
	}
	
	public void putVoiceOutputData(VoicePriorityData voiceData)
	{
		synchronized(voiceOutputDataQueue)
		{
			this.voiceOutputDataQueue.offer(voiceData);
		}
	}
	
	public VoicePriorityData getVoiceOutputData()
	{
		synchronized(voiceOutputDataQueue)
		{
			return this.voiceOutputDataQueue.poll();
		}
	}
	
	protected boolean write() throws IOException {
		// TODO Auto-generated method stub
		VoicePriorityData voiceData = getVoiceOutputData();
		
		if(voiceData==null)
			return false;
		
		String phoneNumber = Session.getInstance().getPhoneNumber();
		
		if(phoneNumber == null)
			return false;
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(C.VOICE_MAX_PACKET_SIZE);
		
		//write phoneNumber
		CharBuffer charBuffer = CharBuffer.wrap(Session.getInstance().getPhoneNumber());
		charsetEncoder.encode(charBuffer, byteBuffer, true);
	
		//write packet type
		byteBuffer.put(C.PACKET_TYPE_DATA);
			
		//write seq
		byteBuffer.putInt(voiceData.seqNumber);
		
		//write from flag
		byteBuffer.put(fromFlag);
		
		//write timestamp
		byteBuffer.putInt(Utils.getSystemTimeInteger());
		
		//write voiceData
		byteBuffer.put(voiceData.voiceData);
		
		//write length tag
		byteBuffer.putInt(byteBuffer.position());
		
		if(this.type==C.TYPE_TCP)
			byteBuffer.putLong(PacketTokenizer.EOF_VALUE);
		
		byteBuffer.flip();
		
		if(this.type==C.TYPE_TCP)
		{
			//Log.i("scpark", "write seqNumber = " + voiceData.seqNumber);
			while(byteBuffer.remaining()>0)
				transmissionChannel.write(byteBuffer, null);
		}
		
		else if(this.type==C.TYPE_UDP)
		{
			if(Session.getInstance().isEnabledFrom(C.FROM_UDP_PRIVATE))
			{
				byteBuffer.put(C.FROM_FLAG_POSITION, C.FROM_UDP_PRIVATE);
				long ipLong = Session.getInstance().getVoiceIPLong(C.FROM_UDP_PRIVATE);
				int port = Session.getInstance().getVoicePort(C.FROM_UDP_PRIVATE);
				String ipString = Utils.longToIp(ipLong);
				transmissionChannel.write(byteBuffer, new InetSocketAddress(ipString, port));
				byteBuffer.flip();
			}
			
			if(Session.getInstance().isEnabledFrom(C.FROM_UDP_PUBLIC))
			{
				byteBuffer.put(C.FROM_FLAG_POSITION, C.FROM_UDP_PUBLIC);
				long ipLong = Session.getInstance().getVoiceIPLong(C.FROM_UDP_PUBLIC);
				int port = Session.getInstance().getVoicePort(C.FROM_UDP_PUBLIC);
				String ipString = Utils.longToIp(ipLong);
				transmissionChannel.write(byteBuffer, new InetSocketAddress(ipString, port));
				byteBuffer.flip();
			}
			
			if(Session.getInstance().isEnabledFrom(C.FROM_UDP_SERVER))
			{
				byteBuffer.put(C.FROM_FLAG_POSITION, C.FROM_UDP_SERVER);
				long ipLong = Session.getInstance().getVoiceIPLong(C.FROM_UDP_SERVER);
				int port = Session.getInstance().getVoicePort(C.FROM_UDP_SERVER);
				int portCount = Session.getInstance().getVoicePortCount(C.FROM_UDP_SERVER);
				String ipString = Utils.longToIp(ipLong);
				if(portCount>0)
					transmissionChannel.write(byteBuffer, new InetSocketAddress(ipString, port + random.nextInt(portCount)));
				byteBuffer.flip();
			}
		}
	
		if(sendIPCount<C.IP_SEND_AMOUNT && (this.fromFlag==C.FROM_TCP_SERVER_MAIN || this.fromFlag==C.FROM_UDP_SERVER))
		{
			sendIPCount++;
			return sendIPs();
		}
		
	    return true;
	}
	
	public boolean sendIPs()
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(C.VOICE_MAX_PACKET_SIZE);
		
		//write phoneNumber
		CharBuffer charBuffer = CharBuffer.wrap(Session.getInstance().getPhoneNumber());
		charsetEncoder.encode(charBuffer, byteBuffer, true);
		
		//write packet type
		byteBuffer.put(C.PACKET_TYPE_IP);
				
		//write IP
		String privateAddress = getIPAddress(true);
		if(privateAddress==null || privateAddress.length()==0)
			return false;
		
		long ipLong = Utils.ipToLong(privateAddress);
		byteBuffer.putLong(ipLong);
		
		//write port
		int port = getPrivatePort();
		if(port<0)
			return false;
		
		byteBuffer.putInt(port);
		
		//write length tag
		byteBuffer.putInt(byteBuffer.position());
		
		if(this.type==C.TYPE_TCP)
			byteBuffer.putLong(PacketTokenizer.EOF_VALUE);
		
		byteBuffer.flip();
		
		if(this.type==C.TYPE_UDP)
		{
			ipLong = Session.getInstance().getVoiceIPLong(fromFlag);
			port = Session.getInstance().getVoicePort(fromFlag);
			
			SocketAddress address = new InetSocketAddress(Utils.longToIp(ipLong), port);
			try {
				transmissionChannel.write(byteBuffer, address);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return false;
			}
			return true;
		}
		else if(this.type==C.TYPE_TCP)
		{
			try {
				while(byteBuffer.remaining()>0)
					transmissionChannel.write(byteBuffer, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return false;
			}
			return true;
		}
			
		return false;
	}
	
	protected boolean close() throws IOException
	{
		if(transmissionChannel!=null)
			transmissionChannel.close();
		
		voiceOutputDataQueue.reset();
		if(selector!=null)
			selector.close();
		
		connected = false;
		return true;
	}
	
	protected boolean read() throws IOException {
		// TODO Auto-generated method stub
		ByteBuffer byteBuffer = ByteBuffer.allocate(C.VOICE_MAX_PACKET_SIZE);
		byte[] byteArray = new byte[C.VOICE_DATA_SIZE];
		
		SocketAddress socketAddress = transmissionChannel.read(byteBuffer);
		//Log.i("scpark", "read1");
		if(this.type==C.TYPE_UDP && socketAddress==null)
			return false;
		
		byteBuffer.flip();
		
		if(this.type==C.TYPE_TCP)
		{
			packetTokenizer.put(byteBuffer);
			byteBuffer = packetTokenizer.get();
		}
		//Log.i("scpark", "read2");
		while(byteBuffer!=null)
		{
			if(byteBuffer.limit()<C.VOICE_MIN_PACKET_SIZE)
				return false;
			
			//check length
			if(byteBuffer.limit()-C.LENGTH_TAG_SIZE!=byteBuffer.getInt(byteBuffer.limit()-C.LENGTH_TAG_SIZE))
				return false;
			//Log.i("scpark", "read3");
			byteBuffer.limit(byteBuffer.limit()-C.LENGTH_TAG_SIZE);
					
			//get phoneNumber
			byte[] phoneNumberArray = new byte[SessionInfo.PHONE_NUMBER_LENGTH];
			byteBuffer.get(phoneNumberArray);
			
			//get packetType
			byte packetType = byteBuffer.get();
			if(packetType!=C.PACKET_TYPE_DATA)
				return false;
			//Log.i("scpark", "read4");
			//get seqNumber
			int seq = byteBuffer.getInt();
			
			//get from flag
			byte fromFlag = byteBuffer.get();
			
			//get time stamp
			int timeStamp = byteBuffer.getInt();
			
			Session.getInstance().setLatency(fromFlag, Utils.getSystemTimeInteger() - timeStamp);
			
			//get voice data
			byte[] newVoiceData = new byte[byteBuffer.limit() - byteBuffer.position()];
			byteBuffer.get(newVoiceData);
		
			//VoicePriorityData voicePriorityData = new VoicePriorityData(seq, newVoiceData);
			//Session.getInstance().putVoiceInputData(voicePriorityData);
			OpenSLAudioPlayer.getInstance().put(seq, newVoiceData);
			if(this.type==C.TYPE_TCP)
				byteBuffer = packetTokenizer.get();
			else
				byteBuffer = null;
		}
		//Log.i("scpark", "read5");
		
		if(this.type==C.TYPE_UDP)
		{
			InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
			InetAddress inetAddress = inetSocketAddress.getAddress();
			long ipLong = Utils.ipToLong(inetAddress.getHostAddress()); 
			if(ipLong == Session.getInstance().getVoiceIPLong(C.FROM_UDP_SERVER)) //from server
				Session.getInstance().setUdpServerInsert();
		}
		
		return true;
	}
	
	public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4) 
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
	
	private int getPrivatePort() {
		if(this.type==C.TYPE_TCP)
			return ((Socket)transmissionChannel.socket()).getLocalPort();
		else if(this.type==C.TYPE_UDP)
			return ((DatagramSocket)transmissionChannel.socket()).getLocalPort();
		
		return -1;
	}
	
	public void doStart()
	{
		if(state==STATE_STOPPED)
			this.start();
	}
	
	public void doStop()
	{
		if(state==STATE_RUNNING)
		{
			this.transmissionState = TRANSMISSIONSTATE_STOPPED;
			this.state = STATE_STOPPED;
		}
	}
}
