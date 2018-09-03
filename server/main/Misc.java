package main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class Misc {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		for(int i=0;i<100;i++)
		{
			SocketChannel socketChannel = SocketChannel.open();
			LOG.I(socketChannel.hashCode()+"");
			socketChannel.socket().bind(new InetSocketAddress((new Random()).nextInt(10000)+10000));
			LOG.I(socketChannel.hashCode()+"");
		}
	}

}
