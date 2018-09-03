package session;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dropbox.core.DbxException;

import main.Buffer;
import main.BufferPool;
import main.C;

import main.LOG;
import main.MyThread;
import main.R;
import main.Unit;
import main.Utils;
import voice.HandlerData;
import voice.PacketTokenizer;

public class SessionManager extends MyThread implements Unit {
	static SessionManager instance;
	
	ServerSocketChannel serverSocketChannel;
	Selector selector;
	Random random;
	
	public CopyOnWriteArrayList<Session> sessionList;
	public ConcurrentHashMap<SocketChannel, Session> sessionMapBySocketChannel;
	public ConcurrentHashMap<SocketChannel, PacketTokenizer> tokenizerMapByVoiceSocketChannel;
	public ConcurrentHashMap<Session, SessionHandler> sessionHandlerMapBySession;
	public ConcurrentHashMap<Long, Session> sessionMapByPhoneNumber;
	public ConcurrentHashMap<Long, Session> sessionMapByPrankKey;
	
	private SessionHandler[] sessionHandlers;
	
	
	HashMap<String, ArrayList<String>> nickNameListMap;
	
	long lastLoopTime;
	/*
	private ConcurrentHashMap<String, PhoneNumberItem> phoneNumberAllocMapByPhoneNumber;
	private ConcurrentHashMap<Long, PhoneNumberItem> phoneNumberAllocMapByPrankKey;
	*/

	private Connection dbConnection;
	private static final String PRANK_KEY = "prankKey";
	private static final String PHONE_NUMBER = "phoneNumber";
	private static final String DATE = "date";
	
	
	
	public static SessionManager getInstance()
	{
		if(instance==null)
			try {
				instance = new SessionManager();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		
		return instance;
	}
	
	private SessionManager() throws ClassNotFoundException, SQLException
	{
		
		random = new Random();
		
		sessionList = new CopyOnWriteArrayList<Session>();
		sessionMapByPrankKey = new ConcurrentHashMap<Long, Session>();
		sessionMapByPhoneNumber = new ConcurrentHashMap<Long, Session>();
		sessionHandlerMapBySession = new ConcurrentHashMap<Session, SessionHandler>();
		sessionMapBySocketChannel = new ConcurrentHashMap<SocketChannel, Session>();
		//sessionMapByVoiceSocketChannel = new ConcurrentHashMap<SocketChannel, Session>();
		tokenizerMapByVoiceSocketChannel = new ConcurrentHashMap<SocketChannel, PacketTokenizer>();
		
		//outputSessionQueue = new ConcurrentLinkedQueue<Session>();
		
		
		sessionHandlers = new SessionHandler[C.SESSION_HANDLER_COUNT];
		for(int i=0;i<sessionHandlers.length;i++)
			(sessionHandlers[i] = new SessionHandler(i)).start();
		
		nickNameListMap = new HashMap<String, ArrayList<String>>();
		
		try {
			nickNameListMap.put("KOR", this.loadNicknames("kor"));
			nickNameListMap.put("USA", this.loadNicknames("eng"));
			nickNameListMap.put("JPN", this.loadNicknames("jpn"));
			nickNameListMap.put("DEU", this.loadNicknames("deu"));
			nickNameListMap.put("FRA", this.loadNicknames("fra"));
			nickNameListMap.put("RUS", this.loadNicknames("rus"));
			nickNameListMap.put("ITA", this.loadNicknames("ita"));
			nickNameListMap.put("ESP", this.loadNicknames("esp"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		
		Class.forName("org.sqlite.JDBC");
		dbConnection = DriverManager.getConnection("jdbc:sqlite:prankcalldb");
		dbConnection.setAutoCommit(false);
		//readPhoneNumberInfomations();
	    System.out.println("Opened database successfully");
	    
	  
	}
	

	@Override
	public void run()
	{
		try {
			init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			try {
				printServerIP();
			} catch (DbxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(-1);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				System.exit(-1);
			}
		
		
		state = STATE_RUNNING;
		while(state==STATE_RUNNING)
		{	
			lastLoopTime = System.currentTimeMillis();
			
			// select
			try {
				selector.select(1000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("SELECTOR ERROR");
				continue;
			}
			
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
					
			while(iterator.hasNext())
			{
				SelectionKey selectionKey = (SelectionKey) iterator.next();
			
				Buffer buffer = null;
				//accept
				try {
					
					
					if(selectionKey.isAcceptable())
						accept(selectionKey);
				
					//read
					if(selectionKey.isReadable())
					{
						SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
						buffer = BufferPool.getInstance().allocate();
						//buffer.addTag("SessionManager40");
						int n = 0;
						try
						{
							n = socketChannel.read(buffer.byteBuffer);	
						}catch(Exception e)
						{
							//LOG.E("READ FAIL");
						}	
						
						SessionHandler sessionHandler = null;
						Session session = this.sessionMapBySocketChannel.get(socketChannel);
						
						if(session!=null)
						{
							sessionHandler = this.sessionHandlerMapBySession.get(session);
							if(sessionHandler!=null)
							{
								if(n>0)
								{
									buffer.byteBuffer.flip();
									session.putInputBuffer(buffer);
									sessionHandler.queueSession(session);
								}
								else
								{
									ConnectionMonitor.getInstance().addErrorCount(session, R.ERROR);
									BufferPool.getInstance().deallocate(buffer);
								}
							}
							else
							{
								BufferPool.getInstance().deallocate(buffer);
								close(socketChannel);
							}
						}
						else
						{
							BufferPool.getInstance().deallocate(buffer);
							close(socketChannel);
						}
						
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					if(buffer!=null)
						BufferPool.getInstance().deallocate(buffer);
					//LOG.E("IOException KEY");
					
				} catch (CancelledKeyException e) {
					if(buffer!=null)
						BufferPool.getInstance().deallocate(buffer);
					//LOG.E("CancelledKey");
				}
				iterator.remove();
			}
		}
	}
	



	private void printServerIP() throws DbxException, IOException {
		// TODO Auto-generated method stub
		try
		{
		    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
		    {
		        NetworkInterface intf = en.nextElement();
		        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
		        {
		            InetAddress inetAddress = enumIpAddr.nextElement();
		            if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && !inetAddress.isSiteLocalAddress()
		            		&& !inetAddress.isAnyLocalAddress() && !inetAddress.isMulticastAddress())
		            {
		            	String serverIP = inetAddress.getHostAddress();
		            	System.out.println(serverIP);
		            	C.SERVER_IP = Utils.ipToLong(serverIP);
		            	
		            	//DropBox.uploadAddress(Utils.ipToLong(serverIP)+"/"+C.SESSION_PORT);
		            	//C.SERVER_IP = Utils.ipToLong("192.168.0.6");
		            	return;
		            }
		        }
		    }
		}
		catch (SocketException ex) {}
	}

	private void init() throws IOException
	{
		selector = Selector.open();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(C.SESSION_PORT));
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	private void accept(SelectionKey selectionKey) throws IOException
	{
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		
		if(socketChannel==null)
			return;
		
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);
		
		Session session = SessionPool.getInstance().allocate();
		if(session==null)
		{
			//LOG.E("sessionPool is full.");
			socketChannel.close();
			return;
		}
		
		SessionHandler sessionHandler = sessionHandlers[random.nextInt(sessionHandlers.length)]; 
		session.init(socketChannel, sessionHandler);
		this.sessionList.add(session);
		
		
		Session getSession = this.sessionMapBySocketChannel.get(socketChannel);
		if(getSession!=null)
		{
			//LOG.E("exist hash code");
		}
		
		this.sessionMapBySocketChannel.put(socketChannel, session);
		this.sessionHandlerMapBySession.put(session, sessionHandler);
		//this.sessionMapByPhoneNumber.put(session.sessionInfo.phoneNumber, session);
		//this.sessionMapBySessionKey.put(System.identityHashCode(socketChannel), session);
	}
	
	
	
	
	
	public void close(SocketChannel socketChannel) {
		// TODO Auto-generated method stub
		synchronized(socketChannel)
		{
			Session session = this.sessionMapBySocketChannel.get(socketChannel);
			if(session!=null)
				ConnectionMonitor.getInstance().queueInvalidSession(session);
			else
				synchronized(socketChannel)
				{
					try {
						SelectionKey key = socketChannel.keyFor(selector);
						if(key!=null)
							socketChannel.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
		}
	}
	
	public void closeSession(Session session)
	{
		if(session==null)
			return;
		
		try {
			session.OnClose();
			synchronized(session.socketChannel)
			{
				SelectionKey key = session.socketChannel.keyFor(selector);
				if(key!=null)
					session.socketChannel.keyFor(selector).cancel();
				session.socketChannel.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		sessionHandlerMapBySession.remove(session);
		sessionList.remove(session);
		sessionMapBySocketChannel.remove(session.socketChannel);
		
		long hashCode = Utils.getPhoneNumberHashCode(session.sessionInfo.phoneNumber);
		if(hashCode<0)
		{
			sessionMapByPhoneNumber.remove(hashCode);
			SessionPool.getInstance().deallocate(session);
			return;
		}
		Session sessionInMap = sessionMapByPhoneNumber.get(hashCode);
		if(sessionInMap==session)
			sessionMapByPhoneNumber.remove(hashCode);
	
		
		SessionPool.getInstance().deallocate(session);
		//LOG.I("Session("+ session.sessionInfo.phoneNumber + ") is closed.");
	}
	
	
	
	/*
	public void queueOutputSession(Session session)
	{
		this.outputSessionQueue.offer(session);
	}
	*/

	public long makeNewPrankKey()
	{
		long newLong = 0;
		boolean exist = true;
		
		Statement statement = null;
		try
		{
			statement = dbConnection.createStatement();
			String sql = "SELECT * from phoneNumberAlloc where prankKey=";
			
			while(exist)
			{
				newLong = random.nextLong();
				if(newLong == 0)
					continue;
				
				ResultSet resultSet = statement.executeQuery(sql + newLong);
				if(resultSet.next())
					exist = true;
				else
					exist = false;
			}
			statement.close();
			
			
		} catch(Exception e)
		{
			return 0;
		}
		
		/*
		while(exist)
		{
			newLong = random.nextLong();
			if(newLong == 0)
				continue;
			
			if(this.phoneNumberAllocMapByPrankKey.get(newLong)==null)
				exist = false;
		}
		*/
		allocatePrankKey(newLong);
		return newLong;
	}

	
	public String allocNewPhoneNumber(long prankKey)
	{
		String newPhoneNumber = null;
		do
		{
			long newPhoneNumberLong = random.nextLong() % 10000000000L;
			if(newPhoneNumberLong<0)
				continue;
			
			newPhoneNumber = "0" + String.format("%010d", newPhoneNumberLong);
		}
		while(isExistPhoneNumber(newPhoneNumber));
			
		if(!allocPhoneNumber(prankKey, newPhoneNumber))
		{
			LOG.E("alloc fail");
			return null;
		}
		return newPhoneNumber; 
	}
	
	private boolean allocPhoneNumber(long prankKey, String phoneNumber)
	{
		
		try {
			synchronized(dbConnection)
			{
			
				Statement statement = this.dbConnection.createStatement();
				String sql = "UPDATE phoneNumberAlloc set phoneNumber='" + phoneNumber + "', " +
							"date=" + System.currentTimeMillis() +
							" where prankKey=" + prankKey;
				statement.executeUpdate(sql);
				statement.close();
				dbConnection.commit();
			}
			return true;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//LOG.E("allocPhoneNumber ERROR " + prankKey);
			return false;
		}
	}
	
	private boolean isExistPhoneNumber(String phoneNumber)
	{
		boolean exist = false;
		
		try {
			synchronized(dbConnection)
			{
				Statement statement = dbConnection.createStatement();
				String sql = "SELECT * FROM phoneNumberAlloc WHERE phoneNumber='" + phoneNumber + "';";
				
				ResultSet resultSet = statement.executeQuery(sql);
				if(resultSet.next())
					exist = true;
				
				statement.close();
				resultSet.close();
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return exist;
	}

	
	public String makeNickname(String country)
	{
		ArrayList<String> nickNameList = this.nickNameListMap.get(country);
		if(nickNameList==null)
			nickNameList = this.nickNameListMap.get("USA");
		
		return nickNameList.get(random.nextInt(nickNameList.size()));
	}
	
	private ArrayList<String> loadNicknames(String country) throws IOException
	{
		ArrayList<String> newNickNamesList = new ArrayList<String>();
		BufferedReader bufferedReader = new BufferedReader(new FileReader("flowername_"+country));
		String string;
		
		while((string = bufferedReader.readLine())!=null)
			newNickNamesList.add(string);
		
		bufferedReader.close();
		return newNickNamesList;
	}
	
	public int getUserCount()
	{
		return this.sessionList.size();
	}

	public ArrayList<Session> getUserList(Session mySession, int userCount, String country) {
		// TODO Auto-generated method stub
		
		ArrayList<Session> returnSessionList = new ArrayList<Session>();
		
		int count = 0;
		for(int i=0;i<1000;i++)
		{
			int listIndex = random.nextInt(sessionList.size());
			Session session = sessionList.get(listIndex);
			
			if(session==mySession)
				continue;
			
			if(session.state!=Session.STATE_STARTED)
				continue;
			
			if(country!=null && !session.sessionInfo.country.equals(country))
				continue;
			
			if(returnSessionList.contains(session))
				continue;
			
			returnSessionList.add(session);
			count++;
			
			if(count>userCount)
				break;
		}
		
		return returnSessionList;
	}
	
	/*
	public String getUserList(Session mySession, int userCount) {
		// TODO Auto-generated method stub
		
		for(int i=0;i<userCount;i++)
		{
			int listIndex = random.nextInt(sessionList.size());
			Session session = sessionList.get(listIndex);
			returnSessionList[i] = session;
			
			
			if(session==mySession)
				continue;
			
			if(session.state!=Session.STATE_STARTED)
				continue;
			
			boolean preExist = false;
			for(int j=0;j<i;j++)
				if(returnSessionList[j].sessionInfo.phoneNumber==session.sessionInfo.phoneNumber)
				{
					preExist = true;
					break;
				}
			
			if(!preExist)
			{
				String userData = session.getUserListData();
				sb.append(userData);
			}
		}
		
		return sb.toString();
	}
	*/
	private void allocatePrankKey(long prankKey) {
		// TODO Auto-generated method stub
		//this.phoneNumberAllocMapByPrankKey.put(prankKey, new PhoneNumberItem(prankKey, null, System.currentTimeMillis()));
		
		try {
			synchronized(dbConnection)
			{
				Statement statement = this.dbConnection.createStatement();
				String sql = "INSERT INTO phoneNumberAlloc (prankKey, date) VALUES (" + prankKey + ", " + System.currentTimeMillis() + ");";
				statement.executeUpdate(sql);
				statement.close();
				dbConnection.commit();
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//LOG.E("allocationPrankKey ERROR " + prankKey);
		}
		
	}

	
	public boolean isPrankKeyExist(long prankKey) {
		// TODO Auto-generated method stub
		
		int rowCount = 0;
		try {
			synchronized(dbConnection)
			{
			
				Statement statement = this.dbConnection.createStatement();
				String sql = "UPDATE phoneNumberAlloc set date=" + System.currentTimeMillis() +
							" where prankKey=" + prankKey;
				
				rowCount = statement.executeUpdate(sql);
				statement.close();
				dbConnection.commit();
			}
			
			if(rowCount==0)
				return false;
			else
				return true;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//LOG.E("allocPhoneNumber ERROR " + prankKey);
			return false;
		}
	}
	
	public String getPhoneNumberByPrankKey(long prankKey)
	{
		Statement statement;
		try {
			ResultSet resultSet;
			synchronized(dbConnection)
			{
				statement = dbConnection.createStatement();
				
				String sql = "SELECT * FROM phoneNumberAlloc WHERE prankKey = " + prankKey;
				
				resultSet = statement.executeQuery(sql);
			}
			
			if(!resultSet.next())
				return null;
			
			
			String phoneNumber = resultSet.getString(PHONE_NUMBER);
		
			statement.close();
			resultSet.close();
			
			return phoneNumber;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	
	public void showInfo(String phoneNumber)
	{
		Session session = this.sessionMapByPhoneNumber.get(Utils.getPhoneNumberHashCode(phoneNumber));
	}

	public Session getSessionByPhoneNumber(String phoneNumber) {
		// TODO Auto-generated method stub
		long hashCode = Utils.getPhoneNumberHashCode(phoneNumber);
		if(hashCode<0)
			return null;
		
		Session session = this.sessionMapByPhoneNumber.get(hashCode);
		return session;
	}




	public String[] getUserData(String phoneNumber) {
		// TODO Auto-generated method stub
		long hashCode = Utils.getPhoneNumberHashCode(phoneNumber);
		if(hashCode<0)
			return null;
		
		Session session = this.sessionMapByPhoneNumber.get(hashCode);
		if(session==null)
			return null;
		else
			return session.getUserData();
	}
	
	public int getCallingSessionCount()
	{
		int count = 0;
		for(Session session:this.sessionList)
		{
			if(session.state==Session.STATE_CALLING)
				count++;
		}
		
		return count;
	}
	
	public void printSessionsState()
	{
		
		for(Session session:this.sessionList)
		{
			LOG.I("state : " + session.state);
		}
		
		
	}

	@Override
	public String deallocTest() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder(1024);
		sb.append("SessionManager - ");
		
		synchronized(sessionList)
		{
			sb.append("sessionList : ");
			sb.append(sessionList.size());
		}
		
		synchronized(sessionMapBySocketChannel)
		{
			sb.append(" sessionMapBySocketChannel : ");
			sb.append(sessionMapBySocketChannel.size());
		}
		
		synchronized(tokenizerMapByVoiceSocketChannel)
		{
			sb.append(" tokenizerMapByVoiceSocketChannel : ");
			sb.append(tokenizerMapByVoiceSocketChannel.size());
		}
		
		synchronized(sessionHandlerMapBySession)
		{
			sb.append(" sessionHandlerMapBySession : ");
			sb.append(sessionHandlerMapBySession.size());
		}
		
		synchronized(sessionMapByPhoneNumber)
		{
			sb.append(" sessionMapByPhoneNumber : ");
			sb.append(sessionMapByPhoneNumber.size());
		}
		
		synchronized(sessionMapByPrankKey)
		{
			sb.append(" sessionMapByPrankKey : ");
			sb.append(sessionMapByPrankKey.size());
		}
		sb.append("\n");
		return sb.toString();
	}

	public SessionHandler[] getSessionHandlers()
	{
		return sessionHandlers;
	}

	
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return this.lastLoopTime + C.THREAD_ALIVE_TIME > System.currentTimeMillis();
	}
	
	@Override
	public String getTag() {
		// TODO Auto-generated method stub
		return "SessionManager";
	}
}
