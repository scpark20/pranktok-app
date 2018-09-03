package voice;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import main.LOG;

public class HandlerData {
	public SocketChannel socketChannel;
	public DatagramChannel datagramChannel;
	public ByteBuffer byteBuffer;
	public SocketAddress socketAddress;
	
	public HandlerData(DatagramChannel datagramChannel, ByteBuffer byteBuffer, SocketAddress socketAddress)
	{
		this.socketChannel = null;
		this.datagramChannel = datagramChannel;
		this.byteBuffer = byteBuffer;
		this.socketAddress = socketAddress;
		
	}
	
	public HandlerData(SocketChannel socketChannel, ByteBuffer byteBuffer, SocketAddress socketAddress)
	{
		this.socketChannel = socketChannel;
		this.datagramChannel = null;
		this.byteBuffer = byteBuffer;
		this.socketAddress = socketAddress;
		
	}
}
