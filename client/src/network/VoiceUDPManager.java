package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.MyThread;
import com.scpark.prankcallclient.Utils;

import session.Session;
import session.SessionInfo;
import android.util.Log;

public class VoiceUDPManager extends VoiceManager {
	int startTime;
	
	public VoiceUDPManager()
	{
		super(C.TYPE_UDP, C.FROM_UDP_SERVER);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		connected = true;
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{
			connected = false;
			//Log.i("scpark", "thread VoiceUDPManager1");
			if(transmissionState==TRANSMISSIONSTATE_STARTED)
			{
				try {
					init();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					continue;
				}
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
			
			startTime = Utils.getSystemTimeInteger();
			while(transmissionState == TRANSMISSIONSTATE_STARTED)
			{
				//Log.i("scpark", "thread VoiceUDPManager2");
				try {
					selector.select(1000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					continue;
				}
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				
				while(iterator.hasNext())
				{
					SelectionKey selectionKey = (SelectionKey) iterator.next();
					
					//read
					if(selectionKey.isReadable())
					{
						try {
							read();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							//e1.printStackTrace();
						}
					
					}	
					
					if(selectionKey.isWritable())
					{
						try {
							write();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
					iterator.remove();
				}
			}
			
			try {
					close();
			} catch (IOException e) {
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
	
	private void init() throws IOException
	{
		selector = Selector.open();
		transmissionChannel = transmissionChannel.open(C.TYPE_UDP);
		DatagramSocket datagramSocket = (DatagramSocket) transmissionChannel.socket();
		SocketAddress address = new InetSocketAddress(0);
		datagramSocket.setReuseAddress(true);
		datagramSocket.bind(address);
		transmissionChannel.configureBlocking(false);
		transmissionChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		
		connected = true;
	}	
}
