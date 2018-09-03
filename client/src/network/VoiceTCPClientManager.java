package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Random;

import android.util.Log;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.MyThread;
import com.scpark.prankcallclient.Utils;

import session.Session;

public class VoiceTCPClientManager extends VoiceManager {
	
	public VoiceTCPClientManager(byte fromFlag)
	{
		super(C.TYPE_TCP, fromFlag);
	}
	
	@Override
	public void run()
	{
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{
			connected = false;
			//Log.i("scpark", "thread VoiceTCPClientManager1");
			if(transmissionState==TRANSMISSIONSTATE_STARTED)
			{
				try {
					init();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						//e1.printStackTrace();
					}
					continue;
				}
				
				OnConnected();
			}
			else
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				continue;
			}
			
			while(transmissionState==TRANSMISSIONSTATE_STARTED)
			{
				//Log.i("scpark", "thread VoiceTCPClientManager2");
				try {
					selector.select(1000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while(iterator.hasNext())
				{
					SelectionKey selectionKey = iterator.next();
					
					if(selectionKey.isReadable())
						try {
							read();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					
					if(selectionKey.isWritable())
						try {
							write();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					
					iterator.remove();
				}
				
			}
			
			try {
				if(connected)
					close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		try {
			close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	public void init() throws IOException
	{
		transmissionChannel = TransmissionChannel.open(C.TYPE_TCP);
		transmissionChannel.configureBlocking(false);
		((Socket)transmissionChannel.socket()).setReuseAddress(true);
		long ipLong = Session.getInstance().getVoiceIPLong(fromFlag);
		String ipString = Utils.longToIp(ipLong);
		int port = Session.getInstance().getVoicePort(fromFlag);
		transmissionChannel.connect(new InetSocketAddress(ipString, port));
		
		for(int i=0;i<100;i++)
		{
			connected = transmissionChannel.finishConnect();
			if(connected)
				break;
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		if(!connected)
		{
			transmissionChannel.close();
			throw new IOException();
		}
		
		selector = Selector.open();
		transmissionChannel.register(selector,  SelectionKey.OP_READ | SelectionKey.OP_WRITE);	
	}
	
	public void OnConnected()
	{
		Session.getInstance().OnVoiceTcpServerConnected(((Socket)transmissionChannel.socket()).getLocalPort());
	}
}
