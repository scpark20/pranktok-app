package session;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import main.BufferPool;
import main.C;
import main.LOG;
import main.MyThread;
import main.Unit;
import main.Utils;
import voice.VoiceTcpManager;
import voice.VoiceUdpManager;

public class ConnectionMonitor extends MyThread implements Unit {
	static private ConnectionMonitor instance = null;
	
	private ConcurrentHashMap<Long, ArrayList<Session>> connectedIPMap;
	private ConcurrentHashMap<Session, Integer> errorCountMap;
	private ConcurrentLinkedQueue<Session> invalidSessionQueue;
	
	VoiceUdpManager[] voiceUdpManagers = new VoiceUdpManager[C.VOICE_UDP_PORT_COUNT];
	VoiceTcpManager[] voiceTcpManagers = new VoiceTcpManager[C.VOICE_TCP_PORT_COUNT];
	ArrayList<Unit> unitList;
	
	Random random = new Random();
	long lastLoopTime;
	
	static public ConnectionMonitor getInstance() {
		// TODO Auto-generated method stub
		if(instance==null)
			instance = new ConnectionMonitor();
		
		return instance;
	}
	
	public ConnectionMonitor()
	{
		connectedIPMap = new ConcurrentHashMap<Long, ArrayList<Session>>();
		errorCountMap = new ConcurrentHashMap<Session, Integer>();
		invalidSessionQueue = new ConcurrentLinkedQueue<Session>();
	
		int port = 0;
		int i = 0;
		while(i<C.VOICE_UDP_PORT_COUNT)
		{
			try {
				voiceUdpManagers[i] = new VoiceUdpManager(C.VOICE_UDP_PORT+port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				port++;
				System.exit(-1);
				continue;
			}
			//voiceUdpManagers[i].setPriority(Thread.MAX_PRIORITY);
			voiceUdpManagers[i].start();
			
			port++;
			i++;
		}
		
		port = 0;
		i = 0;
		while(i<C.VOICE_TCP_PORT_COUNT)
		{
			try {
				voiceTcpManagers[i] = new VoiceTcpManager(C.VOICE_TCP_PORT+port);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				port++;
				System.exit(-1);
				continue;
			}
			//voiceTcpManagers[i].setPriority(Thread.MAX_PRIORITY);
			voiceTcpManagers[i].start();
			port++;
			i++;
		}
		
		unitList = new ArrayList<Unit>();
		unitList.add(this);
		unitList.add(BufferPool.getInstance());
		unitList.add(SessionPool.getInstance());
		unitList.addAll(SessionPool.getInstance().getSessionList());
		unitList.add(SessionManager.getInstance());
		unitList.addAll(Arrays.asList(SessionManager.getInstance().getSessionHandlers()));
		unitList.addAll(Arrays.asList(voiceUdpManagers));
		unitList.addAll(Arrays.asList(voiceTcpManagers));
	
	}
	
	@Override
	public void run()
	{
		state = STATE_RUNNING;
		
		while(state == STATE_RUNNING)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			long currentTime = System.currentTimeMillis();
			lastLoopTime = currentTime;
			
			for(Session session: SessionManager.getInstance().sessionList)
			{
				
				if(session.lastHeartBeatTime + C.HEART_BEAT_TIME_OUT < currentTime)
				{
					//LOG.E("session " + session.getUserListData() + " doesn't response heart beat.");
					queueInvalidSession(session);
				}
				
				if(session.connectTime + C.SESSION_START_TIME < currentTime && session.state == Session.STATE_INITED)
				{
					queueInvalidSession(session);
				}
			}
			
			// delete invalid sessions
			Session session = invalidSessionQueue.poll();
			while(session!=null)
			{
				SessionManager.getInstance().closeSession(session);
				session = invalidSessionQueue.poll();
			}
			
			synchronized(errorCountMap)
			{
				for(Entry<Session, Integer> entry: errorCountMap.entrySet())
				{
					
					session = entry.getKey();
					int errorCount = entry.getValue();
					//LOG.I("monitoring errorCountMap " + Utils.longToIp(session.ipLong));
					if(errorCount<C.ERROR_COUNT_MAXIMUM)
					{
						SessionManager.getInstance().closeSession(session);
						errorCountMap.remove(session);
					}
				}
			}
		}
	}
	
	public void queueInvalidSession(Session session)
	{
		this.invalidSessionQueue.offer(session);
	}
	
	public void putConnectedIP(long ipLong, Session session) {
		// TODO Auto-generated method stub
		synchronized(connectedIPMap)
		{
			ArrayList<Session> sessionList = this.connectedIPMap.get(ipLong);
			if(sessionList==null)
			{
				sessionList = new ArrayList<Session>();
				
				Utils.doCommand("/usr/sbin/ufw insert 1 allow from " + Utils.longToIp(ipLong) + " to any port " + C.VOICE_UDP_PORT + ":" + (C.VOICE_UDP_PORT+C.VOICE_UDP_PORT_COUNT-1) + " proto udp");
				Utils.doCommand("/usr/sbin/ufw insert 1 allow from " + Utils.longToIp(ipLong) + " to any port " + C.VOICE_TCP_PORT + ":" + (C.VOICE_TCP_PORT+C.VOICE_TCP_PORT_COUNT-1) + " proto tcp");
			}
				
			sessionList.add(session);
			this.connectedIPMap.put(ipLong, sessionList);
		}
	}
	
	public boolean isValidIP(long ipLong)
	{
		return connectedIPMap.containsKey(ipLong);
	}
	
	public void removeConnectedIP(long ipLong, Session session)
	{
		synchronized(connectedIPMap)
		{
			ArrayList<Session> sessionList = this.connectedIPMap.get(ipLong);
			if(sessionList==null)
				return;
			
			sessionList.remove(session);
			if(sessionList.isEmpty())
			{
				this.connectedIPMap.remove(ipLong);

				if(sessionList.isEmpty())
				{
					Utils.doCommand("/usr/sbin/ufw delete allow from " + Utils.longToIp(ipLong) + " to any port " + C.VOICE_UDP_PORT + ":" + (C.VOICE_UDP_PORT+C.VOICE_UDP_PORT_COUNT-1) + " proto udp");
					Utils.doCommand("/usr/sbin/ufw delete allow from " + Utils.longToIp(ipLong) + " to any port " + C.VOICE_TCP_PORT + ":" + (C.VOICE_TCP_PORT+C.VOICE_TCP_PORT_COUNT-1) + " proto tcp");
				}
			}
		}
	}
	
	public void removeSessionInErrorCountMap(Session session)
	{
		synchronized(errorCountMap)
		{
			this.errorCountMap.remove(session);
		}
	}
	
	
	public void registerErrorCount(Session session)
	{
		synchronized(errorCountMap)
		{
			errorCountMap.put(session, 0);
		}
	}
	
	public void addErrorCount(Session session, int addCount)
	{
		if(session.state==Session.STATE_ENDED)
			return;
		
		synchronized(errorCountMap)
		{
			Integer errorCount = errorCountMap.get(session);
			if(errorCount==null)
				return;
			
			errorCount += addCount;
			
			errorCountMap.put(session, errorCount);
		}
	}
	
	public VoiceTcpManager getActiveVoiceTcpManager()
	{
		for(int i=0;i<100;i++)
		{
			int index = random.nextInt(C.VOICE_TCP_PORT_COUNT);
			if(voiceTcpManagers[index].isRunning())
				return voiceTcpManagers[index];
		}
		
		return null;
	}
	
	public VoiceUdpManager[] getActiveVoiceUdpManagers()
	{
		VoiceUdpManager[] ret = new VoiceUdpManager[C.VOICE_UDP_PORT_OFFER_COUNT];
 		
		for(int i=0;i<100;i++)
		{
			int index = random.nextInt(C.VOICE_UDP_PORT_COUNT-C.VOICE_UDP_PORT_OFFER_COUNT+1);
			if(voiceUdpManagers[index].isRunning())
			{
				for(int j=0;j<ret.length;j++)
					ret[j] = voiceUdpManagers[index+j];
			}
		}

		return ret;
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder(1024);
		sb.append("ConnectionMonitor - ");
		
		synchronized(connectedIPMap)
		{
			sb.append("connectedIPMap : ");
			sb.append(connectedIPMap.size());
		}
		
		synchronized(errorCountMap)
		{
			sb.append(" errorCountMap : ");
			sb.append(errorCountMap.size());
		}
		
		synchronized(invalidSessionQueue)
		{
			sb.append(" invalidSessionQueue : ");
			sb.append(invalidSessionQueue.size());
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
		return "ConnectionMonitor";
	}
	
	public void runDeallocTest()
	{
		LOG.C("runDeallocTest start");
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter("runDeallocTest.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return;
		}
		
		if(out==null)
			return;
		
		for(Unit unit: this.unitList)
		{
			if(unit==null)
				continue;
			
			String string = unit.deallocTest();
			try {
				out.write(string);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				break;
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return;
		}
		
		LOG.C("runDeallocTest end");
	}
	
	public LinkedList<String> getCheckRunningString()
	{
		LinkedList<String> returnList = new LinkedList<String>();
		for(Unit unit: this.unitList)
		{
			if(unit==null)
				continue;
			
			if(unit instanceof Session)
				continue;
			
			StringBuilder sb = new StringBuilder(128);
			boolean running = unit.isRunning();
			sb.append(unit.getTag());
			sb.append(" ");
			sb.append(running);
			sb.append("\n");
			returnList.add(sb.toString());
		}
		
		return returnList;
	}

	public void runCheckRunning()
	{
		LOG.C("runCheckRunning start");
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter("runCheckRunning.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return;
		}
		
		if(out==null)
			return;
		
		for(Unit unit: this.unitList)
		{
			if(unit==null)
				continue;
			
			if(unit instanceof Session)
				continue;
			
			boolean running = unit.isRunning();
			try {
				out.write(unit.getTag() + " " + running + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				break;
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return;
		}
		
		LOG.C("runCheckRunning end");
	}
}
