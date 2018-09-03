package session;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import com.scpark.prankcallclient.C;
import com.scpark.prankcallclient.CallingFragment;

import com.scpark.prankcallclient.MainActivity;
import com.scpark.prankcallclient.MainFragment;
import com.scpark.prankcallclient.ModifyProfileFragment;
import com.scpark.prankcallclient.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import network.AESCrypto;
import network.Address;
import network.Crypto;
import network.RSACrypto;
import network.SessionManager;
import network.VoiceManager;
import network.VoiceTCPClientManager;
import network.VoiceTCPServerManager;
import network.VoiceUDPManager;
import voice.VoicePriorityData;
import voice.VoicePriorityList;

public class Session {
	static private Session instance = null;
	
	private String SERVER_IP;
	private int SERVER_PORT;
	
	private static String PREF_SERVER_IP = "fewijfiwejgij3g";
	public static String PREF_SERVER_IP_DEFAULT = "0";
	
	private static String PREF_SERVER_PORT = "b4kmgik4mntgk4qngkl4ng";
	public static int PREF_SERVER_PORT_DEFAULT = 0;

	private static String PREF_MY_PHONENUMBER = "EKJF#ITIJT";
	private static String PREF_MY_PHONENUMBER_DEFAULT = "";
	
	private static String PREF_MY_NICKNAME = "3k2jt23hgtuh";
	private static String PREF_MY_NICKNAME_DEFAULT = "";
	
	private static String PREF_MY_PRANKKEY = "fkejwfk2j3";
	private static long PREF_MY_PRANKKEY_DEFAULT = 0;
	
	private static String PREF_MY_EMOJI = "fewfaawej";
	private static int PREF_MY_EMOJI_DEFAULT = 0;
	
	private static final String PREF_MY_COUNTRY_ONLY = "Fewguhgut3";
	private static final boolean PREF_MY_COUNTRY_ONLY_DEFAULT = false;
	
	private HashMap<String, int[]> flagCodeMap;
	public ArrayList<Integer> faceItemList;
	
	SessionInfo sessionInfo;
	public boolean myCountryOnly;
	public ConcurrentLinkedQueue<String> outputMessageQueue;
	private long lastOutputMessageTime = 0;
	VoicePriorityList voiceInputDataQueue;

	public long lastHeartBeatTime;
	public boolean heartBeatSended;
	private boolean latencyChecked;
	
	private static final ScheduledExecutorService latencyCheckExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private boolean[] voiceFromFlag = new boolean[C.FROM_COUNT];
	
	private long[] voiceIP = new long[C.FROM_COUNT];
	private int[] voicePort = new int[C.FROM_COUNT];
	private int[] voicePortCount = new int[C.FROM_COUNT];
	
	private double[] latencies = new double[C.FROM_COUNT];
	private long[] insertTimes = new long[C.FROM_COUNT];
	private double[] averageGaps = new double[C.FROM_COUNT];
	private long udpServerInsertTime;
	private double udpServerAverageGap; 
	//private double[] variances = new double[C.FROM_COUNT];
	
	private VoiceManager[] voiceManagers;
	
	private static final int TRANSMISSION_COUNT = 5;
	
	private static final int TCP_OWN_SERVER_INDEX = 0;
	private static final int TCP_TO_PRIVATE_INDEX = 1;
	private static final int TCP_TO_PUBLIC_INDEX = 2;
	private static final int TCP_TO_SERVER_INDEX = 3;
	private static final int UDP_INDEX = 4;
	
	public RSACrypto myRSACrypto = null;
	public AESCrypto myAESCrypto = null;
	public AESCrypto myAESVoiceCrypto = null;
	
	public RSACrypto serverRSACrypto = null;
	public AESCrypto serverAESCrypto = null;
	
	public RSACrypto otherRSACrypto = null;
	public AESCrypto otherAESCrypto = null;
	
	//String calledFromPhoneNumber = null;
	
	public int state = STATE_ENDED;
	
	public static final int STATE_INITED = -1;
	public static final int STATE_STARTED = 0;
	public static final int STATE_WAITCALL = 1;
	public static final int STATE_CALLING = 2;
	public static final int STATE_RECEIVEDCALL = 3;
	public static final int STATE_ENDED = 4;
	
	private static final double LATENCY_FILTER_COEFF = 0.75;
	private static final double LATENCY_DEFAULT_VALUE = 9999.9999;
	public Context context;
	
	public DBHelper buddiesDB;
	private Random random = new Random();
	
	static public Session getInstance()
	{
		if(instance == null)
			instance = new Session();
		
		return instance;
	}
	
	private Session()
	{
		
	}
	
	public void onCreate(Context context)
	{
		if(state==STATE_CALLING)
			return;
		
		this.context = context;
		
		outputMessageQueue = new ConcurrentLinkedQueue<String>();
		voiceInputDataQueue = new VoicePriorityList(C.VOICE_BUFFER_QUEUE_SIZE);
		lastHeartBeatTime = System.currentTimeMillis();
		voiceManagers = new VoiceManager[TRANSMISSION_COUNT];
		
		flagCodeMapInit();
		emojiCodeInit();
		
		SharedPreferences sharedPref = context.getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		
		this.SERVER_IP = sharedPref.getString(PREF_SERVER_IP, PREF_SERVER_IP_DEFAULT);
		this.SERVER_PORT = sharedPref.getInt(PREF_SERVER_PORT, PREF_SERVER_PORT_DEFAULT);
		
		long prankKey = sharedPref.getLong(PREF_MY_PRANKKEY, PREF_MY_PRANKKEY_DEFAULT);
		int emojiCode = sharedPref.getInt(PREF_MY_EMOJI, PREF_MY_EMOJI_DEFAULT);
		if(emojiCode==PREF_MY_EMOJI_DEFAULT)
			emojiCode = faceItemList.get(random.nextInt(faceItemList.size()));
		
		String nickName = sharedPref.getString(PREF_MY_NICKNAME, PREF_MY_NICKNAME_DEFAULT);
		String phoneNumber = sharedPref.getString(PREF_MY_PHONENUMBER, PREF_MY_PHONENUMBER_DEFAULT);
		String country = context.getResources().getConfiguration().locale.getISO3Country();
		myCountryOnly = sharedPref.getBoolean(PREF_MY_COUNTRY_ONLY, PREF_MY_COUNTRY_ONLY_DEFAULT); 
				
		this.sessionInfo = new SessionInfo(prankKey, nickName, phoneNumber, emojiCode, country);
		
		buddiesDB = new DBHelper(context);
		
		for(int i=0;i<voiceFromFlag.length;i++)
			voiceFromFlag[i] = false;
		
		state = STATE_INITED;
	}
	
	private void emojiCodeInit() {
		// TODO Auto-generated method stub
		faceItemList = new ArrayList<Integer>();
		faceItemList.add(0x1F600);
		faceItemList.add(0x1F601); faceItemList.add(0x1F602); faceItemList.add(0x1F603); faceItemList.add(0x1F604);
		faceItemList.add(0x1F605); faceItemList.add(0x1F606); faceItemList.add(0x1F607); faceItemList.add(0x1F608); 
		faceItemList.add(0x1F609); faceItemList.add(0x1F60A); faceItemList.add(0x1F60B); faceItemList.add(0x1F60C);
		faceItemList.add(0x1F60D); faceItemList.add(0x1F60E); faceItemList.add(0x1F60F); faceItemList.add(0x1F610);
		
		faceItemList.add(0x1F611); faceItemList.add(0x1F612); faceItemList.add(0x1F613); faceItemList.add(0x1F614);
		faceItemList.add(0x1F615); faceItemList.add(0x1F616); faceItemList.add(0x1F617); faceItemList.add(0x1F618); 
		faceItemList.add(0x1F619); faceItemList.add(0x1F61A); faceItemList.add(0x1F61B); faceItemList.add(0x1F61C);
		faceItemList.add(0x1F61D); faceItemList.add(0x1F61E); faceItemList.add(0x1F61F); faceItemList.add(0x1F620);
		
		faceItemList.add(0x1F608); faceItemList.add(0x1F47F); faceItemList.add(0x1F479); faceItemList.add(0x1F47A);
		faceItemList.add(0x1F480); faceItemList.add(0x2620); faceItemList.add(0x1F47B); faceItemList.add(0x1F47D);
		faceItemList.add(0x1F47E); faceItemList.add(0x1F916); faceItemList.add(0x1F4A9); faceItemList.add(0x1F63A);
		faceItemList.add(0x1F638); faceItemList.add(0x1F639); faceItemList.add(0x1F63B); faceItemList.add(0x1F63C);
		
		faceItemList.add(0x1F63D); faceItemList.add(0x1F640); faceItemList.add(0x1F63F); faceItemList.add(0x1F63E);
		faceItemList.add(0x1F648); faceItemList.add(0x1F649); faceItemList.add(0x1F64A); faceItemList.add(0x1F466);
	}

	public void OnNetworkConnected()
	{
		outputMessageQueue.clear();
		
		this.myRSACrypto = new RSACrypto();
		this.myAESCrypto = null;
		this.myAESVoiceCrypto = null;
		
		this.otherAESCrypto = null;
		this.otherRSACrypto = null;
		
		this.serverRSACrypto = null;
		this.serverAESCrypto = null;
		
		String args[] = {this.myRSACrypto.getPublicKeyString()};
		String operationString = buildOperation(SessionOps.PUT_PUBLIC_KEY, args);
		outputMessageQueue.add(operationString);
	}
	
	
	private void OnVersionOK()
	{
		state = STATE_STARTED;
		
		String[] args = new String[]{sessionInfo.country + ""};
		String operationString = buildOperation(SessionOps.PUT_COUNTRY, args);
		outputMessageQueue.add(operationString);
		
		if(sessionInfo.prankKey == PREF_MY_PRANKKEY_DEFAULT)
		{
			operationString = buildOperation(SessionOps.GET_NEWPRANKKEY, null);
			outputMessageQueue.add(operationString);
		}
		else
		{
			args = new String[]{sessionInfo.prankKey+""};
			operationString = buildOperation(SessionOps.PUT_PRANKKEY, args);
			outputMessageQueue.add(operationString);
		}
		
		if(sessionInfo.nickName.equals(PREF_MY_NICKNAME_DEFAULT))
		{
			operationString = buildOperation(SessionOps.GET_NEWNICKNAME, null);
			outputMessageQueue.add(operationString);
		}
		else
		{
			args = new String[]{sessionInfo.nickName};
			operationString = buildOperation(SessionOps.PUT_NICKNAME, args);
			outputMessageQueue.add(operationString);
		}
		
		args = new String[]{sessionInfo.emojiCode + ""};
		operationString = buildOperation(SessionOps.PUT_EMOJI, args);
		outputMessageQueue.add(operationString);
		
		getUserList(C.USER_LIST);
	}
	
	private void OnVersionError()
	{
		if(context==null)
			return;
		
		final String appPackageName = context.getPackageName(); // getPackageName() from Context or Activity object
		try {
		    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
		} catch (android.content.ActivityNotFoundException anfe) {
		    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
		}
	}

	public void putInputMessage(String inputMessage) 
	{
		// TODO Auto-generated method stub
		//LOG.I("Input Message : " + inputMessage);
		String[] operationArray = inputMessage.split(C.OPERATION_DELIMITER_STRING);
		
		if(operationArray.length==0)
			return;
		
		for(String operation: operationArray)
			putOperation(operation);
	}
	
	private void putOperation(String operation)
	{
		String[] recordArray = operation.split(C.RECORD_DELIMITER_STRING);
		
		if(recordArray.length==0)
			return;
		
		String opString = recordArray[0];
		
		if(opString.equals(SessionOps.PUT_PUBLIC_KEY))
		{
			if(recordArray.length!=2)
				return;
			
			this.serverRSACrypto = new RSACrypto(recordArray[1]);
		
			myAESCrypto = new AESCrypto();
			String[] args = {this.myAESCrypto.getSecretKeyString()};
			String operationString = buildOperation(SessionOps.PUT_SECRET_KEY, args);
			outputMessageQueue.add(operationString);
		}
		
		else if(opString.equals(SessionOps.PUT_SECRET_KEY))
		{
			if(recordArray.length!=2)
				return;
			
			this.serverAESCrypto = new AESCrypto(recordArray[1]);

			String[] args = {C.CURRENT_VERSION+""};
			String operationString = buildOperation(SessionOps.PUT_CURRENT_VERSION, args);
			outputMessageQueue.add(operationString);
		}
		
		else if(opString.equals(SessionOps.VERSION_OK))
		{
			if(recordArray.length!=1)
				return;
			
			this.OnVersionOK();
		}
		else if(opString.equals(SessionOps.VERSION_ERROR))
		{
			if(recordArray.length!=1)
				return;
			
			this.OnVersionError();
		}
		
		else if(opString.equals(SessionOps.PUT_PRANKKEY))
		{
			if(recordArray.length < 2)
				return;
			
			this.sessionInfo.prankKey = Long.parseLong(recordArray[1]);
			
			SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putLong(PREF_MY_PRANKKEY, sessionInfo.prankKey);
			editor.commit();
			
			String operationString = buildOperation(SessionOps.GET_NEWPHONENUMBER, null);
			outputMessageQueue.add(operationString);	
			
			//MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_REFRESHMYNICKNAME);
		}
		
		else if(opString.equals(SessionOps.INVALID_PRANKKEY))
		{
			String operationString = buildOperation(SessionOps.GET_NEWPRANKKEY, null);
			outputMessageQueue.add(operationString);
		}
		
		else if(opString.equals(SessionOps.PUT_NICKNAME))
		{
			if(recordArray.length < 2)
				return;
			
			this.sessionInfo.nickName = recordArray[1];
			
			SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(PREF_MY_NICKNAME, sessionInfo.nickName);
			editor.commit();
			
			if(MainFragment.getInstance()!=null)
			{
				Message message = new Message();
				message.what = MainFragment.MSG_SETNICKNAME;
				message.obj = sessionInfo.nickName;
				MainFragment.getInstance().handler.sendMessage(message);
			}
			
			if(ModifyProfileFragment.getInstance()!=null)
			{
				Message message = new Message();
				message.what = ModifyProfileFragment.MSG_SETNICKNAME;
				message.obj = sessionInfo.nickName;
				ModifyProfileFragment.getInstance().handler.sendMessage(message);
			}
			
		}
		
		else if(opString.equals(SessionOps.PUT_UDP_IPs))
		{
			if(state!=STATE_CALLING)
				return;
			
			if(recordArray.length!=5)
				return;
			
			this.voiceIP[C.FROM_UDP_PRIVATE] = Long.parseLong(recordArray[1]);
			this.voicePort[C.FROM_UDP_PRIVATE] = Integer.parseInt(recordArray[2]);
			voiceFromFlag[C.FROM_UDP_PRIVATE] = true;
			
			this.voiceIP[C.FROM_UDP_PUBLIC] = Long.parseLong(recordArray[3]);
			this.voicePort[C.FROM_UDP_PUBLIC] = Integer.parseInt(recordArray[4]);
			voiceFromFlag[C.FROM_UDP_PUBLIC] = true;
			
			/*
			if(!this.voiceManagers[UDP_INDEX].isTransmissionStarted())
				this.voiceManagers[UDP_INDEX].startTransmission();
			*/
		}
		
		else if(opString.equals(SessionOps.PUT_UDP_SERVER_IP))
		{
			if(state!=STATE_CALLING)
				return;
			
			if(recordArray.length!=4)
				return;
			
			this.voiceIP[C.FROM_UDP_SERVER] = Long.parseLong(recordArray[1]);
			this.voicePort[C.FROM_UDP_SERVER] = Integer.parseInt(recordArray[2]);
			this.voicePortCount[C.FROM_UDP_SERVER] = Integer.parseInt(recordArray[3]);
			voiceFromFlag[C.FROM_UDP_SERVER] = true;
			
			
			if(!this.voiceManagers[UDP_INDEX].isTransmissionStarted())
				this.voiceManagers[UDP_INDEX].startTransmission();
				
		}
		
		else if(opString.equals(SessionOps.PUT_TCP_IPs))
		{
			if(state!=STATE_CALLING)
				return;
			
			if(recordArray.length!=5)
				return;
			
			this.voiceIP[C.FROM_TCP_PRIVATE] = Long.parseLong(recordArray[1]);
			this.voicePort[C.FROM_TCP_PRIVATE] = Integer.parseInt(recordArray[2]);
			voiceFromFlag[C.FROM_TCP_PRIVATE] = true;
			
			this.voiceIP[C.FROM_TCP_PUBLIC] = Long.parseLong(recordArray[3]);
			this.voicePort[C.FROM_TCP_PUBLIC] = Integer.parseInt(recordArray[4]);
			voiceFromFlag[C.FROM_TCP_PUBLIC] = true;
			
			if(!this.voiceManagers[TCP_TO_PRIVATE_INDEX].isTransmissionStarted())
				this.voiceManagers[TCP_TO_PRIVATE_INDEX].startTransmission();
			
			if(!this.voiceManagers[TCP_TO_PUBLIC_INDEX].isTransmissionStarted())
				this.voiceManagers[TCP_TO_PUBLIC_INDEX].startTransmission();
			
		}
		
		else if(opString.equals(SessionOps.PUT_TCP_SERVER_IP))
		{
			if(state!=STATE_CALLING)
				return;
			
			if(recordArray.length!=3)
				return;
			
			this.voiceIP[C.FROM_TCP_SERVER_MAIN] = Long.parseLong(recordArray[1]);
			this.voicePort[C.FROM_TCP_SERVER_MAIN] = Integer.parseInt(recordArray[2]);
			
			voiceFromFlag[C.FROM_TCP_SERVER_MAIN] = true;
			
			
			if(!this.voiceManagers[TCP_TO_SERVER_INDEX].isTransmissionStarted())
				this.voiceManagers[TCP_TO_SERVER_INDEX].startTransmission();
			
			
		}
		
		
		
		
		else if(opString.equals(SessionOps.PUT_PHONENUMBER))
		{
			if(recordArray.length < 2)
				return;
			
			this.sessionInfo.phoneNumber = recordArray[1];
			
			SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(PREF_MY_PHONENUMBER, sessionInfo.phoneNumber);
			editor.commit();
			
			
			if(MainFragment.getInstance()!=null)
			{
				Message message = new Message();
				message.what = MainFragment.MSG_SETPHONENUMBER;
				message.obj = sessionInfo.phoneNumber;
				MainFragment.getInstance().handler.sendMessage(message);
			}
			
			if(ModifyProfileFragment.getInstance()!=null)
			{
				Message message = new Message();
				message.what = ModifyProfileFragment.MSG_SETPHONENUMBER;
				message.obj = sessionInfo.phoneNumber;
				ModifyProfileFragment.getInstance().handler.sendMessage(message);
			}
			//MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_REFRESHMYPHONENUMBER);
		}
		
		else if(opString.equals(SessionOps.HEART_BEAT))
		{
			this.lastHeartBeatTime = System.currentTimeMillis();
			heartBeatSended = false;
		}
		
		else if(opString.equals(SessionOps.PUT_USERLISTSTART))
		{
			if(MainFragment.getInstance()!=null)
				MainFragment.getInstance().handler.sendEmptyMessage(MainFragment.MSG_LISTCLEAR);
		}
		
		else if(opString.equals(SessionOps.PUT_USERINFO))
		{
			if(recordArray.length != 5 && recordArray.length != 2)
				return;
			
			if(recordArray.length==5)
			{
				String nickName = recordArray[1];
				String phoneNumber = recordArray[2];
				int emojiCode = Integer.parseInt(recordArray[3]);
				String country = recordArray[4];
				
				if(MainFragment.getInstance()!=null)
				{
					Message message = new Message();
					message.what = MainFragment.MSG_PUTBUDDYUSERLIST;
					SessionInfo newSessionInfo = new SessionInfo(nickName, phoneNumber, emojiCode, country, true, true);
					message.obj = newSessionInfo;
					this.buddiesDB.write(newSessionInfo);
					MainFragment.getInstance().handler.sendMessage(message);
				}
			}
			else if(recordArray.length==2)
			{
				String phoneNumber = recordArray[1];
				if(MainFragment.getInstance()!=null)
				{
					Message message = new Message();
					message.what = MainFragment.MSG_PUTBUDDYUSERNONLIST;
					message.obj = phoneNumber;
					MainFragment.getInstance().handler.sendMessage(message);
				}
			}
		}
		
		else if(opString.equals(SessionOps.PUT_USERLIST))
		{
			if(recordArray.length != 5)
				return;
			
			String nickName = recordArray[1];
			String phoneNumber = recordArray[2];
			int emojiCode = Integer.parseInt(recordArray[3]);
			String country = recordArray[4];
			
			if(MainFragment.getInstance()!=null)
			{
				Message message = new Message();
				message.what = MainFragment.MSG_PUTUSERLIST;
				message.obj = new SessionInfo(nickName, phoneNumber, emojiCode, country);
				MainFragment.getInstance().handler.sendMessage(message);
			}
		}
		
		else if(opString.equals(SessionOps.PUT_USERLISTEND))
		{
			if(MainFragment.getInstance()!=null)
				MainFragment.getInstance().handler.sendEmptyMessage(MainFragment.MSG_LISTUPDATED);
		}
		
		else if(opString.equals(SessionOps.CALLED_FROM))
		{
			if(recordArray.length!=7)
				return;
			
			if(state != STATE_STARTED)
				return;
			
			String nickName = recordArray[1];
			String phoneNumber = recordArray[2];
			int emojiCode = Integer.parseInt(recordArray[3]);
			String country = recordArray[4];
			String publicKey = recordArray[5];
			String secretKey = null;
			try {
				secretKey = this.myRSACrypto.decode(recordArray[6]);
			} catch (ShortBufferException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			}
			
			if(secretKey==null)
				return;
			
			this.otherRSACrypto = new RSACrypto(publicKey);
			this.otherAESCrypto = new AESCrypto(secretKey);
			
			SessionInfo newSessionInfo = new SessionInfo(nickName, phoneNumber, emojiCode, country);
			Message msg = new Message();
			msg.what = MainActivity.MSG_CALLEDFROM;
			msg.obj = newSessionInfo;
			MainActivity.getInstance().handler.sendMessage(msg);
			
			state = STATE_RECEIVEDCALL;
		}
		
		else if(opString.equals(SessionOps.ENABLE_FROM))
		{
			if(state!=STATE_CALLING)
				return;
			
			if(recordArray.length!=8)
				return;
			
			for(int i=0;i<C.FROM_COUNT;i++)
			{
				int value = Integer.parseInt(recordArray[i+1]);
				if(value > 0 && value < C.ENABLE_ONLY_THRESHOLD)
					voiceFromFlag[i] = true;
				else
					voiceFromFlag[i] = false;
			}
			
			boolean[] voiceToFlag = new boolean[C.FROM_COUNT];
			for(int i=0;i<C.FROM_COUNT;i++)
			{
				double value = averageGaps[i];
				if(value > 0 && value < C.ENABLE_ONLY_THRESHOLD)
					voiceToFlag[i] = true;
				else
					voiceToFlag[i] = false;
			}
			
			boolean voiceFromUdpServer;
			if(this.udpServerAverageGap > 0 && udpServerAverageGap < C.ENABLE_ONLY_THRESHOLD)
				voiceFromUdpServer = true;
			else
				voiceFromUdpServer = false;
		
			//hole punching activated
			if((voiceFromFlag[C.FROM_UDP_PRIVATE] || voiceFromFlag[C.FROM_UDP_PUBLIC] ||
				voiceFromFlag[C.FROM_TCP_PRIVATE] || voiceFromFlag[C.FROM_TCP_PUBLIC] || voiceFromFlag[C.FROM_TCP_SERVER_P2P]) &&
				(voiceToFlag[C.FROM_UDP_PRIVATE] || voiceToFlag[C.FROM_UDP_PUBLIC] ||
				voiceToFlag[C.FROM_TCP_PRIVATE] || voiceToFlag[C.FROM_TCP_PUBLIC] || voiceToFlag[C.FROM_TCP_SERVER_P2P]))
			{
				this.voiceFromFlag[C.FROM_UDP_SERVER] = false;
				voiceManagers[TCP_TO_SERVER_INDEX].stopTransmission();
			}
			else //deactivated
			{
				if(voiceFromUdpServer && voiceFromFlag[C.FROM_UDP_SERVER]) // condition to close tcp to server
					voiceManagers[TCP_TO_SERVER_INDEX].stopTransmission();
			}
			
		}
		
		else if(opString.equals(SessionOps.CALL_ACCEPT))
		{
			if(state != STATE_WAITCALL)
				return;
			
			if(recordArray.length!=2)
				return;
			
			String secretKey = null;
			try {
				secretKey = this.myRSACrypto.decode(recordArray[1]);
			} catch (ShortBufferException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return;
			}
			if(secretKey==null)
				return;
			 
			this.otherAESCrypto = new AESCrypto(secretKey);
			
			this.voiceFromFlag[C.FROM_UDP_SERVER] = true;
			for(int i=0;i<C.FROM_COUNT;i++)
			{
				latencies[i] = LATENCY_DEFAULT_VALUE;
				insertTimes[i] = 0;
				averageGaps[i] = 0;
				
				//variances[i] = 0;
			}
			
			this.udpServerInsertTime = 0;
			this.udpServerAverageGap = 0;
			latencyChecked = false;
			
			state = STATE_CALLING;
			
			if(CallingFragment.getInstance()!=null)
				CallingFragment.getInstance().handler.sendEmptyMessage(CallingFragment.MSG_CALLACCEPTED);
			
			/*
			this.voiceManagers[UDP_INDEX] = new VoiceUDPManager();
			this.voiceManagers[UDP_INDEX].doStart();
			*/
			latencyCheckExecutor.schedule(latencyCheck, C.LATENCY_CHECK_TIME, TimeUnit.MILLISECONDS);
		}
		
		else if(opString.equals(SessionOps.CALL_RESPONSE))
		{
			if(state != STATE_WAITCALL)
				return;
			
			if(recordArray.length!=6)
				return;
			
			String nickName = recordArray[1];
			String phoneNumber = recordArray[2];
			int emojiCode = Integer.parseInt(recordArray[3]);
			String country = recordArray[4];
			String RSACryptoString = recordArray[5];
			
			this.otherRSACrypto = new RSACrypto(RSACryptoString);
			
			SessionInfo destSessionInfo = new SessionInfo(nickName, phoneNumber, emojiCode, country);
			Message msg = new Message();
			msg.what = MainActivity.MSG_CALLRESPONSE;
			msg.obj = destSessionInfo;
			
			MainActivity.getInstance().handler.sendMessage(msg);
			
			myAESVoiceCrypto = new AESCrypto();
			try {
				this.callTo2(otherRSACrypto.encode(this.myAESVoiceCrypto.getSecretKeyString()));
			} catch (ShortBufferException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		
		
		else if(opString.equals(SessionOps.CALL_OFF))
		{
			if(!(state == STATE_CALLING || state == STATE_WAITCALL || state == STATE_RECEIVEDCALL))
				return;
			
			MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_CALLOFFED);
			
			voiceFromFlag[C.FROM_UDP_PRIVATE] = false;
			voiceFromFlag[C.FROM_UDP_PUBLIC] = false;
			voiceFromFlag[C.FROM_UDP_SERVER] = false;
			
			this.stopTransmission();
			this.otherAESCrypto = null;
			this.otherRSACrypto = null;
			state = STATE_STARTED;
		}


		
		else if(opString.equals(SessionOps.WRONG_NUMBER_ERROR))
		{
			if(state != STATE_WAITCALL)
				return;
			
			MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_WRONG_NUMBER_ERROR);
			
			state = STATE_STARTED;
		}
		
		else if(opString.equals(SessionOps.DEST_CALLING_ERROR))
		{
			if(state != STATE_WAITCALL)
				return;
			
			MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_DEST_CALLING_ERROR);
			
			state = STATE_STARTED;
		}
		
		else if(opString.equals(SessionOps.CALL_ACCEPT_ERROR))
		{
			if(state != STATE_WAITCALL)
				return;
			
			state = STATE_STARTED;
		}
		
		else if(opString.equals(SessionOps.CALL_DENY_ERROR))
		{
			if(state != STATE_WAITCALL)
				return;
			
			state = STATE_STARTED;
		}
		
		
	}
	
	private void callTo2(String encodedString) {
		// TODO Auto-generated method stub
		String[] args = {encodedString};
		String operationString = buildOperation(SessionOps.CALL_TO2, args);
		outputMessage(operationString);
	}

	public String getOutputMessage()
	{
		String outputMessage = outputMessageQueue.poll(); 
		//LOG.I("Output Message : " + outputMessage);
		return outputMessage;
	}
	
	public void putVoiceInputData(VoicePriorityData voicePriorityData)
	{
		synchronized(voiceInputDataQueue)
		{
			this.voiceInputDataQueue.offer(voicePriorityData);
		}
	}
	
	public byte[] getVoiceInputData()
	{
		byte[] ret = null;
		synchronized(voiceInputDataQueue)
		{
			VoicePriorityData voicePriorityData = this.voiceInputDataQueue.poll();
	
			if(voicePriorityData != null)
			{
				ret = voicePriorityData.voiceData;
				//Log.i("scpark", voicePriorityData.seqNumber+"");
			}
		}
		
		return ret;
	}
	
	private boolean canOutputMessage()
	{
		if(lastOutputMessageTime + C.OUTPUT_MESSAGE_LATENCY > System.currentTimeMillis())
			return false;
		
		return true;
	}
	
	private void outputMessage(String operationString)
	{
		outputMessageQueue.add(operationString);
		lastOutputMessageTime = System.currentTimeMillis();
	}


	public void getNewNickName() {
		// TODO Auto-generated method stub
		if(!canOutputMessage())
			return;
		
		String operationString = buildOperation(SessionOps.GET_NEWNICKNAME, null);
		outputMessage(operationString);
	}
	
	public void getUserList(int userCount) {
		if(!SessionManager.getInstance().isConnected())
			return;
	
		if(!canOutputMessage())
			return;
	
		SessionInfo[] sessionInfos = buddiesDB.selectAll();
		for(SessionInfo sessionInfo: sessionInfos)
		{
			String[] args = {sessionInfo.phoneNumber};
			String operationString = buildOperation(SessionOps.GET_USERINFO, args);
			outputMessage(operationString);
		}
		
		String[] args = {this.myCountryOnly?"1":"0"};
		String operationString = buildOperation(SessionOps.GET_USERLIST, args);
		outputMessage(operationString);
		
	}

	
	public void getNewPhoneNumber() {
		// TODO Auto-generated method stub
		if(!canOutputMessage())
			return;
		
		String operationString = buildOperation(SessionOps.GET_NEWPHONENUMBER, null);
		outputMessage(operationString);
	}
	
	public void queueHeartBeatOutput() {
		// TODO Auto-generated method stub
		String operationString = buildOperation(SessionOps.HEART_BEAT, null);
		outputMessageQueue.add(operationString);
	}
	
	private String buildOperation(String op, String[] args)
	{
		StringBuilder sb = new StringBuilder(C.SESSION_PACKET_SIZE);
		sb.append(op);
		sb.appendCodePoint(C.RECORD_DELIMITER);
		
		if(args!=null)
			for(String arg: args)
			{
				sb.append(arg);
				sb.appendCodePoint(C.RECORD_DELIMITER);
			}
			
		sb.appendCodePoint(C.OPERATION_DELIMITER);
		return sb.toString();
	}
	

	public void callTo(String phoneNumber) {
		// TODO Auto-generated method stub
		if(!canOutputMessage())
			return;
		
		if(state!=STATE_STARTED)
			return;
		
		String[] args = {phoneNumber + ""};
		String operationString = buildOperation(SessionOps.CALL_TO, args);
		outputMessage(operationString);
		
		state = STATE_WAITCALL;
	}

	public void callOff() {
		// TODO Auto-generated method stub
		String operationString = buildOperation(SessionOps.CALL_OFF, null);
		outputMessageQueue.add(operationString);
		
		voiceFromFlag[C.FROM_UDP_PRIVATE] = false;
		voiceFromFlag[C.FROM_UDP_PUBLIC] = false;
		voiceFromFlag[C.FROM_UDP_SERVER] = false;
		
		this.stopTransmission();
		
		this.otherAESCrypto = null;
		this.otherRSACrypto = null;
		
		state = STATE_STARTED;
	}
	
	
	public void callAccept() {
		// TODO Auto-generated method stub
		for(int i=0;i<C.FROM_COUNT;i++)
		{
			latencies[i] = LATENCY_DEFAULT_VALUE;
			insertTimes[i] = 0;
			averageGaps[i] = 0;
			this.udpServerInsertTime = 0;
			this.udpServerAverageGap = 0;
			//variances[i] = 0;
		}
		
		
		latencyChecked = false;
		
		this.myAESVoiceCrypto = new AESCrypto();
		String[] args = null;
		try {
			args = new String[]{this.otherRSACrypto.encode(myAESVoiceCrypto.getSecretKeyString())};
		} catch (ShortBufferException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		}
		
		if(args==null)
			return;
		
		String operationString = buildOperation(SessionOps.CALL_ACCEPT, args);
		outputMessageQueue.add(operationString);
		
		voiceFromFlag[C.FROM_UDP_SERVER] = true;
		/*
		this.voiceManagers[UDP_INDEX] = new VoiceUDPManager();
		this.voiceManagers[UDP_INDEX].doStart();
		*/
		/*
		if(!this.voiceManagers[UDP_INDEX].isTransmissionStarted())
			this.voiceManagers[UDP_INDEX].startTransmission();
		*/
		latencyCheckExecutor.schedule(latencyCheck, C.LATENCY_CHECK_TIME, TimeUnit.MILLISECONDS);
		this.state = STATE_CALLING;
	}
		
	public int getEmojiPosition(int emojiCode)
	{
		for(int i=0;i<this.faceItemList.size();i++)
			if(emojiCode==faceItemList.get(i))
				return i;
		
		return 0;
	}
	
	public int getEmojiCodeByPosition(int position)
	{
		if(position>=faceItemList.size() || position < 0)
			return faceItemList.get(0);
		
		return faceItemList.get(position);
	}
	
	public int getEmojiCode()
	{
		return this.sessionInfo.emojiCode;
	}
	
	public String getNickName()
	{
		return this.sessionInfo.nickName;
	}

	public String getPhoneNumber()
	{
		return this.sessionInfo.phoneNumber;
	}

	public void putEmojiCode(int emojiCode) {
		// TODO Auto-generated method stub
		this.sessionInfo.emojiCode = emojiCode;
		
		Message msg = new Message();
		msg.what = MainFragment.MSG_SETFACE;
		msg.arg1 = emojiCode;
		MainFragment.getInstance().handler.sendMessage(msg);
		
		String[] args = {sessionInfo.emojiCode + ""};
		String operationString = buildOperation(SessionOps.PUT_EMOJI, args);
		outputMessageQueue.add(operationString);

		SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putInt(PREF_MY_EMOJI, sessionInfo.emojiCode);
		editor.commit();
	}

	public void putNickName(String myNickName) {
		// TODO Auto-generated method stub
		this.sessionInfo.nickName = myNickName;
				
		Message msg = new Message();
		msg.what = MainFragment.MSG_SETNICKNAME;
		msg.obj = myNickName;
		MainFragment.getInstance().handler.sendMessage(msg);
		
		String[] args = {sessionInfo.nickName};
		String operationString = buildOperation(SessionOps.PUT_NICKNAME, args);
		outputMessageQueue.add(operationString);
		
		SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(PREF_MY_NICKNAME, sessionInfo.nickName);
		editor.commit();
		
	}

	public boolean isEnabledFrom(byte fromFlag) {
		// TODO Auto-generated method stub
		return voiceFromFlag[fromFlag];
	}

	public long getVoiceIPLong(byte fromFlag) {
		// TODO Auto-generated method stub
		return voiceIP[fromFlag];
	}

	public int getVoicePort(byte fromFlag) {
		// TODO Auto-generated method stub
		
		return voicePort[fromFlag];
	}
	
	public int getVoicePortCount(byte fromFlag) {
		// TODO Auto-generated method stub
		
		return voicePortCount[fromFlag];
	}

	private void requestEnableOnly(byte fromFlag) {
		// TODO Auto-generated method stub
		//String[] args = {fromFlag+"", + latencies[0]+"", + latencies[1]+"", + latencies[2]+"", latencies[3]+"", latencies[4]+"", latencies[5]+"", latencies[6]+""};
		/*
		String[] args = {fromFlag+"", voiceIP[0]+"", voicePort[0]+"", latencies[0]+"", 
									 voiceIP[1]+"", voicePort[1]+"", latencies[1]+"",
									 voiceIP[2]+"", voicePort[2]+"", latencies[2]+"",
									 voiceIP[3]+"", voicePort[3]+"", latencies[3]+"",
									 voiceIP[4]+"", voicePort[4]+"", latencies[4]+"",
									 voiceIP[5]+"", voicePort[5]+"", latencies[5]+"",
									 voiceIP[6]+"", voicePort[6]+"", latencies[6]+""};
		*/
		String[] args = {fromFlag+""};
		String operationString = buildOperation(SessionOps.ENABLE_FROM, args);
		outputMessageQueue.add(operationString);
	}
	
	public void voiceInputQueueReset()
	{
		this.voiceInputDataQueue.reset();
	}
	
	private void flagCodeMapInit()
	{
		flagCodeMap = new HashMap();
		flagCodeMap.put("AUT", new int[]{0x1F1E6, 0x1F1F9});
		flagCodeMap.put("NLD", new int[]{0x1F1F3, 0x1F1F1});
		flagCodeMap.put("BEL", new int[]{0x1F1E7, 0x1F1EA});
		flagCodeMap.put("NOR", new int[]{0x1F1E6, 0x1F1F9});
		flagCodeMap.put("DNK", new int[]{0x1F1E9, 0x1F1F0});
		
		flagCodeMap.put("PRT", new int[]{0x1F1F5, 0x1F1F9});
		flagCodeMap.put("FRA", new int[]{0x1F1EB, 0x1F1F7});
		flagCodeMap.put("GRC", new int[]{0x1F1EC, 0x1F1F7});
		flagCodeMap.put("ISL", new int[]{0x1F1EE, 0x1F1F8});
		flagCodeMap.put("ITA", new int[]{0x1F1EE, 0x1F1F9});
		
		flagCodeMap.put("LUX", new int[]{0x1F1F1, 0x1F1FA});
		flagCodeMap.put("SWE", new int[]{0x1F1F8, 0x1F1EA});
		flagCodeMap.put("CHE", new int[]{0x1F1E8, 0x1F1ED});
		flagCodeMap.put("TUR", new int[]{0x1F1F9, 0x1F1F7});
		flagCodeMap.put("GBR", new int[]{0x1F1EC, 0x1F1E7});
		
		flagCodeMap.put("DEU", new int[]{0x1F1E9, 0x1F1EA});
		flagCodeMap.put("CAN", new int[]{0x1F1E8, 0x1F1E6});
		flagCodeMap.put("JPN", new int[]{0x1F1EF, 0x1F1F5});
		flagCodeMap.put("FIN", new int[]{0x1F1EB, 0x1F1EE});
		flagCodeMap.put("AUS", new int[]{0x1F1E6, 0x1F1FA});
		
		flagCodeMap.put("NZL", new int[]{0x1F1F3, 0x1F1FF});
		flagCodeMap.put("MEX", new int[]{0x1F1F2, 0x1F1FD});
		flagCodeMap.put("CZE", new int[]{0x1F1E8, 0x1F1FF});
		flagCodeMap.put("HUN", new int[]{0x1F1ED, 0x1F1FA});
		flagCodeMap.put("COL", new int[]{0x1F1E8, 0x1F1F4});
		
		flagCodeMap.put("CRI", new int[]{0x1F1E8, 0x1F1F7});
		flagCodeMap.put("LVA", new int[]{0x1F1F1, 0x1F1FB});
		flagCodeMap.put("LTU", new int[]{0x1F1F1, 0x1F1F9});
		flagCodeMap.put("RUS", new int[]{0x1F1F7, 0x1F1FA});
		flagCodeMap.put("ESP", new int[]{0x1F1EA, 0x1F1F8});
		
		flagCodeMap.put("USA", new int[]{0x1F1FA, 0x1F1F8});
		flagCodeMap.put("POL", new int[]{0x1F1F5, 0x1F1F1});
		flagCodeMap.put("KOR", new int[]{0x1F1F0, 0x1F1F7});
		flagCodeMap.put("SVK", new int[]{0x1F1F8, 0x1F1F0});
		flagCodeMap.put("CHL", new int[]{0x1F1E8, 0x1F1F1});
		
		flagCodeMap.put("SVN", new int[]{0x1F1F8, 0x1F1EE});
		flagCodeMap.put("ISR", new int[]{0x1F1EE, 0x1F1F1});
		flagCodeMap.put("EST", new int[]{0x1F1EA, 0x1F1EA});	
	}
	
	public int[] getFlagCode()
	{
		return flagCodeMap.get(sessionInfo.country);
	}
	
	public int[] getFlagCode(String country)
	{
		return flagCodeMap.get(country);
	}
	
	public String getCountry()
	{
		return sessionInfo.country;
	}

	public void putVoiceOutputData(VoicePriorityData newData) {
		// TODO Auto-generated method stub
		for(int i=0;i<TRANSMISSION_COUNT;i++)
			if(voiceManagers[i]!=null && voiceManagers[i].isConnected())
				voiceManagers[i].putVoiceOutputData(newData);
	}
	
	public void setUdpServerInsert()
	{
		long currentTime = System.currentTimeMillis();
		if(udpServerInsertTime==0)
			udpServerInsertTime = currentTime;
		else
		{
			long currentGap = currentTime - udpServerInsertTime;
			udpServerAverageGap = udpServerAverageGap * 0.99d + ((double)currentGap * 0.01d);
			udpServerInsertTime = currentTime;
		}
	}
	
	public void setLatency(byte fromFlag, double latency)
	{
		if(latencies[fromFlag]==LATENCY_DEFAULT_VALUE)
			latencies[fromFlag] = latency;
		else
			latencies[fromFlag] = latencies[fromFlag] * LATENCY_FILTER_COEFF + latency * (1.0d - LATENCY_FILTER_COEFF);
		
		long currentTime = System.currentTimeMillis(); 
		if(insertTimes[fromFlag]==0)
			insertTimes[fromFlag] = currentTime;
		else
		{
			long currentGap = currentTime - insertTimes[fromFlag];
			averageGaps[fromFlag] = averageGaps[fromFlag] * 0.99d + ((double)currentGap * 0.01d);
			//double currentVariance = currentGap - averageGaps[fromFlag];
			//currentVariance *= currentVariance;
			//variances[fromFlag] = variances[fromFlag] * 0.99d + currentVariance * 0.01d;
			insertTimes[fromFlag] = currentTime;
		}	
	}
	
	public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4) 
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

	public void stopTransmission() {
		// TODO Auto-generated method stub
		for(int i=0;i<TRANSMISSION_COUNT;i++)
		{
			if(this.voiceManagers[i]!=null)
				this.voiceManagers[i].stopTransmission();
		}
		
		//this.voiceManagers[UDP_INDEX].doStop();
	}

	public boolean isAcceptableTCPAddress(long ipLong, int port) {
		// TODO Auto-generated method stub
		if(this.voiceFromFlag[C.FROM_TCP_PRIVATE] && this.voiceIP[C.FROM_TCP_PRIVATE]==ipLong)
				return true;
		else if(this.voiceFromFlag[C.FROM_TCP_PUBLIC] && this.voiceIP[C.FROM_TCP_PUBLIC]==ipLong)
				return true;
		
		return false;
	}

	public void OnVoiceTcpServerConnected(int port) {
		// TODO Auto-generated method stub
		if(!this.voiceManagers[TCP_OWN_SERVER_INDEX].isTransmissionStarted())
		{
			((VoiceTCPServerManager)this.voiceManagers[TCP_OWN_SERVER_INDEX]).setPort(port);
			this.voiceManagers[TCP_OWN_SERVER_INDEX].startTransmission();
		}
	}

	public boolean latencyChecked() {
		// TODO Auto-generated method stub
		return this.latencyChecked;
	}
	
	Runnable latencyCheck = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			String[] args = new String[C.FROM_COUNT];
			
			for(int i=0;i<args.length;i++)
			{
				//requestFrom[i] = (latencies[i]==LATENCY_DEFAULT_VALUE) ? false : true;
				//args[i] = requestFrom[i] ? "1" : "0";
				args[i] = ((int)averageGaps[i])+"";
			}
			String operationString = buildOperation(SessionOps.ENABLE_FROM, args);
			outputMessageQueue.add(operationString);
			latencyChecked = true;
		}
	};

	public void onResume()
	{
		state = STATE_INITED;
		voiceManagers[TCP_OWN_SERVER_INDEX] = new VoiceTCPServerManager();
		voiceManagers[TCP_TO_PRIVATE_INDEX] = new VoiceTCPClientManager(C.FROM_TCP_PRIVATE);
		voiceManagers[TCP_TO_PUBLIC_INDEX] = new VoiceTCPClientManager(C.FROM_TCP_PUBLIC);
		voiceManagers[TCP_TO_SERVER_INDEX] = new VoiceTCPClientManager(C.FROM_TCP_SERVER_MAIN);
		voiceManagers[UDP_INDEX] = new VoiceUDPManager();
		
		for(VoiceManager voiceManager: voiceManagers)
		{
			if(voiceManager!=null)
			{
				//voiceManager.setPriority(Thread.MAX_PRIORITY);
				voiceManager.doStart();
			}
		}
	}

	public void onPause() {
		// TODO Auto-generated method stub
		String operationString = buildOperation(SessionOps.BYE, null);
		outputMessageQueue.add(operationString);
		
		for(VoiceManager voiceManager: voiceManagers)
		{
			if(voiceManager!=null)
				voiceManager.doStop();
		}
		state = STATE_ENDED;
	}

	public void setMyCountryOnly(boolean isChecked) {
		// TODO Auto-generated method stub
		this.myCountryOnly = isChecked;
		SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(PREF_MY_COUNTRY_ONLY, isChecked);
		editor.commit();
	}
	
	public String getServerIP()
	{
		return this.SERVER_IP;
	}
	
	public int getServerPort()
	{
		return this.SERVER_PORT;
	}
	
	public void putServerIP(String serverIP)
	{
		this.SERVER_IP = serverIP;
		SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(this.PREF_SERVER_IP, serverIP);
		editor.commit();
	}
	
	public void putServerPort(int serverPort)
	{
		this.SERVER_PORT = serverPort;
		SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putInt(this.PREF_SERVER_PORT, serverPort);
		editor.commit();
	}
}