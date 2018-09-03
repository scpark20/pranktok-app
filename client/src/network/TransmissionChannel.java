package network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.scpark.prankcallclient.C;

public class TransmissionChannel {
	SocketChannel socketChannel = null;
	DatagramChannel datagramChannel = null;
	
	
	private int type;
	
	
	private TransmissionChannel(int type) throws IOException
	{
		if(type==C.TYPE_UDP)
			datagramChannel = DatagramChannel.open();
		else if(type==C.TYPE_TCP)
			socketChannel = SocketChannel.open();
		
		this.type = type;
	}
	
	private TransmissionChannel(SocketChannel socketChannel, int type) throws IOException
	{
		this.socketChannel = socketChannel;
		this.type = type;
	}
	
	static public TransmissionChannel open(SocketChannel socketChannel, int type) throws IOException
	{
		if(type==C.TYPE_TCP)
			return new TransmissionChannel(socketChannel, type);
		
		return null;
	}
	
	static public TransmissionChannel open(int type) throws IOException
	{
		return new TransmissionChannel(type); 
	}
	
	public int getType()
	{
		return this.type;
	}
	
	public int write(ByteBuffer byteBuffer, SocketAddress socketAddress) throws IOException
	{
		if(this.type==C.TYPE_UDP)
			return datagramChannel.send(byteBuffer, socketAddress);
		else if(this.type==C.TYPE_TCP)
			return socketChannel.write(byteBuffer);
		
		return 0;
	}
	
	public SocketAddress read(ByteBuffer byteBuffer) throws IOException
	{
		if(this.type==C.TYPE_UDP)
			return datagramChannel.receive(byteBuffer);
		else if(this.type==C.TYPE_TCP)
			socketChannel.read(byteBuffer);
		
		return null;
	}
	
	public Object socket()
	{
		if(this.type==C.TYPE_UDP)
			return datagramChannel.socket();
		else if(this.type==C.TYPE_TCP)
			return socketChannel.socket();
		
		return null;
	}

	public Object getChannel()
	{
		if(this.type==C.TYPE_UDP)
			return datagramChannel;
		else if(this.type==C.TYPE_TCP)
			return socketChannel;
		
		return null;
	}
	
	public SelectableChannel configureBlocking(boolean blockingMode) throws IOException
	{
		if(this.type==C.TYPE_UDP)
			return datagramChannel.configureBlocking(blockingMode);
		else if(this.type==C.TYPE_TCP)
			return socketChannel.configureBlocking(blockingMode);
		
		return null;	
	}
	
	public SelectionKey register(Selector selector, int operations) throws ClosedChannelException
	{
		if(this.type==C.TYPE_UDP)
			return datagramChannel.register(selector, operations);
		else if(this.type==C.TYPE_TCP)
			return socketChannel.register(selector, operations);
		
		return null;
	}
	
	public boolean connect(SocketAddress remote) throws IOException
	{
		if(this.type==C.TYPE_TCP)
			return socketChannel.connect(remote);
		
		return false;
	}

	public boolean finishConnect() throws IOException {
		// TODO Auto-generated method stub
		if(this.type==C.TYPE_TCP)
			return socketChannel.finishConnect();
		
		return false;
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
		if(this.type==C.TYPE_UDP)
			datagramChannel.close();
		else if(this.type==C.TYPE_TCP)
			socketChannel.close();
		
	}
}
