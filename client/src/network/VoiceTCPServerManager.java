package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.util.Log;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.Utils;

import session.Session;

public class VoiceTCPServerManager extends VoiceManager {
	ServerSocketChannel serverSocketChannel;
	private int port;

	public VoiceTCPServerManager() {
		super(C.TYPE_TCP, C.FROM_TCP_SERVER_P2P);
	}
	
	@Override
	public void run()
	{
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{
			connected = false;
			//Log.i("scpark", "thread VoiceTCPServerManager1");
			if(transmissionState==TRANSMISSIONSTATE_STARTED)
				try {
					init();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					//e2.printStackTrace();
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
				//Log.i("scpark", "thread VoiceTCPServerManager2");
				// select
				try {
					selector.select(1000);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						//e1.printStackTrace();
					}
					continue;
				}
				
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				
				while(iterator.hasNext())
				{
					SelectionKey selectionKey = (SelectionKey) iterator.next();
				
					//accept
					if(selectionKey.isAcceptable())
						try {
							accept(selectionKey);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					
					//read
					if(selectionKey.isReadable())
						try {
							read();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					
					//write
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
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	@Override
	protected boolean close() throws IOException
	{
		super.close();
		if(serverSocketChannel!=null)
			serverSocketChannel.close();
		
		return true;
	}
	
	private boolean init() throws IOException
	{
		selector = Selector.open();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().setReuseAddress(true);
		serverSocketChannel.socket().bind(new InetSocketAddress(port));
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		connected = true;
		return true;
	}
	
	private void accept(SelectionKey selectionKey) throws IOException
	{
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
		InetAddress inetAddress = inetSocketAddress.getAddress();
		
		long ipLong = Utils.ipToLong(inetAddress.getHostAddress());
		int port = inetSocketAddress.getPort();
		
		if(!Session.getInstance().isAcceptableTCPAddress(ipLong, port))
		{
			socketChannel.socket().close();
			socketChannel.close();
			return;
		}
		
		transmissionChannel = TransmissionChannel.open(socketChannel, C.TYPE_TCP);
		transmissionChannel.configureBlocking(false);
		transmissionChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
}
