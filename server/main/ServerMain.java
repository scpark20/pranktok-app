package main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;

import org.apache.commons.lang3.Conversion;

import session.ConnectionMonitor;
import session.SessionManager;
import session.SessionPool;
import voice.VoiceTcpManager;
import voice.VoiceUdpManager;

public class ServerMain {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BufferPool.getInstance();
		SessionPool.getInstance();
		SessionManager.getInstance().start();
		
		Utils.doCommand("/usr/sbin/ufw deny " + C.VOICE_UDP_PORT + ":" + (C.VOICE_UDP_PORT+C.VOICE_UDP_PORT_COUNT-1) +"/udp");
		Utils.doCommand("/usr/sbin/ufw deny " + C.VOICE_TCP_PORT + ":" + (C.VOICE_TCP_PORT+C.VOICE_TCP_PORT_COUNT-1) +"/tcp");
		
		ConnectionMonitor.getInstance().start();
		
		
		
		Scanner scan = new Scanner(System.in);
		LOG.I("server IP : " + C.SERVER_IP);		
		while(true)
		{
			String input = scan.nextLine();
			String[] inputArray = input.split(" ");
			
			if(inputArray.length==0)
				continue;
			
			if(inputArray[0].equals("uc"))
				LOG.C("userCount : " + SessionManager.getInstance().getUserCount());
			
			if(inputArray[0].equals("info"))
				SessionManager.getInstance().showInfo(inputArray[1]);
			
			if(inputArray[0].equals("b"))
				LOG.C(BufferPool.getInstance().bufferQueue.size()+"");
			
			if(inputArray[0].equals("sqc"))
				LOG.C(SessionPool.getInstance().getQueueSize() + "/" + C.SESSION_ALLOC_SIZE);
			
			if(inputArray[0].equals("cs"))
				LOG.C(SessionManager.getInstance().getCallingSessionCount()+"");
			
			if(inputArray[0].equals("ss"))
				SessionManager.getInstance().printSessionsState();
			
			if(inputArray[0].equals("dt"))
				ConnectionMonitor.getInstance().runDeallocTest();
			
			if(inputArray[0].equals("cr"))
				ConnectionMonitor.getInstance().runCheckRunning();
			
			/*
			if(inputArray[0].equals("fub"))
				BufferPool.getInstance().findUndeallocBuffer();
			*/
		}
	
		
	}

}
