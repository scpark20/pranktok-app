package session;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import main.C;
import main.LOG;
import main.Unit;

public class SessionPool implements Unit {
	static private SessionPool instance = null;
	private Queue<Session> sessionQueue;
	private ArrayList<Session> sessionList;
	Object mutex = new Object();
	int semaphores[];
	
	static public SessionPool getInstance()
	{
		if(instance==null)
			instance = new SessionPool();
		
		return instance;
	}
	
	private SessionPool()
	{
		semaphores = new int[C.SESSION_ALLOC_SIZE];
		sessionQueue = new ConcurrentLinkedQueue<Session>();
		sessionList = new ArrayList<Session>();
		for(int i=0;i<C.SESSION_ALLOC_SIZE;i++)
		{
			Session session = new Session(i);
			sessionQueue.offer(session);
			sessionList.add(session);
			semaphores[i] = 0;
			LOG.I("session("+i+") created.");
		}
		
	}
	
	public Session allocate()
	{
		synchronized(sessionQueue)
		{
			Session session = sessionQueue.poll();
			if(session!=null)
				semaphores[session.sessionID] = semaphores[session.sessionID]+1;
		
		return session;
		}
	}
	
	public boolean deallocate(Session session)
	{
		synchronized(sessionQueue)
		{
			
			
			if(semaphores[session.sessionID]==0)
				return false;
			
			semaphores[session.sessionID] = semaphores[session.sessionID]-1;
			sessionQueue.offer(session);
			
			return true;
		}
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		synchronized(mutex)
		{
			return "sessionQueue size : " + this.sessionQueue.size() + "\n";
		}
	}
	
	public int getQueueSize()
	{
		synchronized(mutex)
		{
			return this.sessionQueue.size();
		}
	}
	
	public ArrayList<Session> getSessionList()
	{
		return this.sessionList;
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return !this.sessionQueue.isEmpty();
	}
	
	@Override
	public String getTag() {
		// TODO Auto-generated method stub
		return "SessionPool";
	}
}
