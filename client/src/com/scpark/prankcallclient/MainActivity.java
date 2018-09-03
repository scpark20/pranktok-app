package com.scpark.prankcallclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import network.Address;
import network.SessionManager;
import network.VoiceUDPManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import session.Session;
import session.SessionInfo;
import session.SessionOps;
import voice.AudioPlayer;
import voice.AudioRecorder;
import voice.OpenSLAudioPlayer;
import voice.VoiceEncoder;

public class MainActivity extends FragmentActivity implements MainFragment.OnMainFragmentLisenter {
	
	AudioManager audioManager;
	FragmentManager fragmentManager;
	static ModifyProfileFragment modifyProfileFragment;
	static PranktokInfoFragment infoFragment;
	static CallingFragment callingFragment;
	
	RelativeLayout mainLayout;
	
	AdAndPhoneLayout adAndPhoneLayout;
	Button[] numberButtonArray;
	String phoneNumberString;
	TextView phoneNumberTV;
	ImageButton backSpaceIB;
	ImageButton refreshIB;
	Button sendCallButton;
	
	Context mainContext;
	
	static final int MSG_NUMBER = 0;
	static final int MSG_BACKSPACE = 1;
	public static final int MSG_CALLEDFROM = 4;
	
	public static final int MSG_CALLACCEPT = 5;
	public static final int MSG_CALLRESPONSE = 6;
	//public static final int MSG_CALLDENY = 7;
	public static final int MSG_CALLOFFED = 8;
	protected static final int MSG_CALLTO = 9;
	
	public static final int MSG_SEARCHPHONENUMBER = 10;
	public static final int MSG_SETPHONENUMBER = 11;
	
	public static final int MSG_REFRESHMYNICKNAME = 12;
	public static final int MSG_REFRESHMYPHONENUMBER = 13;
	
	public static final int MSG_OPENMODIFYPROFILE = 14;
	protected static final int MSG_CLOSECALLINGFRAGMENT = 15;
	
	public static final int MSG_PLAYCALLING = 16;
	public static final int MSG_STOPCALLING = 17;
	
	public static final int MSG_SPEAKERPHONEON = 18;
	public static final int MSG_SPEAKERPHONEOFF = 19;
	public static final int MSG_WRONG_NUMBER_ERROR = 20;
	public static final int MSG_DEST_CALLING_ERROR = 21;
	
	public static final int MSG_OPENINFO = 22;
	
	public int screenState = SCREEN_STATE_LIST;
	public int activityState;
	
	public static final int ACTIVITY_STATE_RESUME = 0;
	public static final int ACTIVITY_STATE_PAUSE = 1;
	
	public static final int SCREEN_STATE_LIST = 0;
	public static final int SCREEN_STATE_PHONE = 1;
	public static final int SCREEN_STATE_CALLING = 2;
	
	public static boolean soundPoolInited = false;
	
	
	Bitmap[] callingBitmapArray = new Bitmap[10];
	
	static MainActivity instance = null;
	//public MediaPlayer mediaPlayer = null;
	static public SoundPool soundPool = null;
	static int startSoundID;
	static int endSoundID;
	//static int callingSoundID;
	//static int callingSoundStreamID;
	int volumeControlStream;
	
	MediaPlayer mediaPlayer;
	
	public static MainActivity getInstance()
	{
		return instance;
	}
	
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		
		if(hasFocus)
		{

			
		}
		else
		{
			
		}
		
		super.onWindowFocusChanged(hasFocus);
	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		this.volumeControlStream = this.getVolumeControlStream();
		this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		
		if(Session.getInstance().state != Session.STATE_CALLING)
		{
			//Log.i("scpark", "thread start");
			Session.getInstance().onResume();
			initThreads();
		}
		
		if(!soundPoolInited)
		{
			soundPool = new SoundPool(5, AudioManager.STREAM_VOICE_CALL, 0);
			startSoundID = soundPool.load(this, R.raw.start, 0);
			endSoundID = soundPool.load(this, R.raw.end, 0);
			//callingSoundID = soundPool.load(this, R.raw.melodycafe, 0);
			soundPoolInited = true;
	    }	
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		
		if(Session.getInstance().state != Session.STATE_CALLING)
		{
			//Log.i("scpark", "thread stop");
			Session.getInstance().onPause();
			this.uninitThreads();
		}
		
		
		
		this.setVolumeControlStream(volumeControlStream);
		
		super.onPause();
		
		
	}


	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//getAddressExecutor.schedule(getAddress, 0, TimeUnit.MILLISECONDS);
		
		instance = this;
		mainContext = this;
		fragmentManager = this.getSupportFragmentManager();
		
		audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		audioManager.setSpeakerphoneOn(false);
		audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, (int)(audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) * 3.0f / 4.0f), 0);

		adAndPhoneLayout = (AdAndPhoneLayout) this.findViewById(R.id.adAndPhoneLayout);
		
		Session.getInstance().onCreate(this);
		setAd();
		
		setPhoneNumberLayout();
		setNumberButton();
		
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		if(Build.VERSION.SDK_INT >= 21)
			window.setStatusBarColor(Color.parseColor("#9C344C"));
		
		if(this.findViewById(R.id.fragment_container) != null)
		{
			if(savedInstanceState!=null)
				return;
			
			MainFragment mainFragment = new MainFragment();
			mainFragment.setArguments(this.getIntent().getExtras());
			this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment).commit();
		}
		
		callingBitmapArray[0] = BitmapFactory.decodeResource(getResources(), R.drawable.blurred);
		callingFragment = new CallingFragment(callingBitmapArray[0]);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
		AssetFileDescriptor afd = this.getResources().openRawResourceFd(R.raw.melodycafe); 
		try {
			mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		} catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		}
		try {
			mediaPlayer.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}


	private void setAd()
	{
		AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
	}
	
	public void initThreads()
	{
		
			int systemSampleRate = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
			OpenSLAudioPlayer.getInstance().init(systemSampleRate, C.SAMPLE_RATE, C.VOICE_DATA_SIZE);
			AudioRecorder.getInstance().doStart();
			AudioPlayer.getInstance().doStart();
			VoiceEncoder.getInstance().doStart();
			SessionManager.getInstance().doStart();
	}
	
	public void uninitThreads()
	{
			int systemSampleRate = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
			OpenSLAudioPlayer.getInstance().uninit();
			AudioRecorder.getInstance().doStop();
			AudioPlayer.getInstance().doStop();
			VoiceEncoder.getInstance().doStop();
			SessionManager.getInstance().doStop();
	}
	
	private void setPhoneNumberLayout()
	{
		numberButtonArray = new Button[10];
        phoneNumberString = "";
        
        phoneNumberTV = (TextView) findViewById(R.id.phoneNumber);
        
        phoneNumberTV.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				String phoneNumber = s.toString().replaceAll("-", "");
				//phoneAdapter.searchPhoneNumber(phoneNumber);
				//phoneAdapter.notifyDataSetChanged();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        
        
        /*
        this.refreshIB = (ImageButton) findViewById(R.id.refreshButton);
        this.refreshIB.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				handler.sendEmptyMessage(MSG_LISTREFRESH);
			}
        	
        });
        */
        this.sendCallButton = (Button) findViewById(R.id.sendCallButton);
        this.sendCallButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String phoneNumber = phoneNumberTV.getText().toString().replaceAll("-", "");
				
				Message msg = new Message();
				msg.what = MSG_CALLTO;
				msg.obj = phoneNumber;
				handler.sendMessage(msg);
				
				
			}
        	
        });
        

	}
	
	
	private void setNumberButton()
    {
    	backSpaceIB = (ImageButton) findViewById(R.id.backspace);
    	
    	backSpaceIB.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_BACKSPACE;
				handler.sendMessage(msg);
			}
    		
    	});
    	
        numberButtonArray[0] = (Button) findViewById(R.id.button0);
        numberButtonArray[1] = (Button) findViewById(R.id.button1);
        numberButtonArray[2] = (Button) findViewById(R.id.button2);
        numberButtonArray[3] = (Button) findViewById(R.id.button3);
        numberButtonArray[4] = (Button) findViewById(R.id.button4);
        numberButtonArray[5] = (Button) findViewById(R.id.button5);
        numberButtonArray[6] = (Button) findViewById(R.id.button6);
        numberButtonArray[7] = (Button) findViewById(R.id.button7);
        numberButtonArray[8] = (Button) findViewById(R.id.button8);
        numberButtonArray[9] = (Button) findViewById(R.id.button9);
    
        numberButtonArray[0].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 0 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[1].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 1 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[2].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 2 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[3].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 3 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[4].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 4 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[5].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 5 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[6].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 6 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[7].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 7 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[8].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 8 + "";
				handler.sendMessage(msg);
			}
        	
        });
        numberButtonArray[9].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_NUMBER;
				msg.obj = 9 + "";
				handler.sendMessage(msg);
			}
        	
        });
      
    }
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		
		switch(screenState)
		{
			case SCREEN_STATE_LIST:
				super.onBackPressed();
				break;
				
			case SCREEN_STATE_PHONE:
				AdAndPhoneLayout.getInstance().handler.sendEmptyMessage(AdAndPhoneLayout.MSG_HIDEPHONELAYOUT);
				break;
				
			case SCREEN_STATE_CALLING:
				break;
		}	
	}
	
	public void setScreenState(int screenState, boolean FORCE)
	{
		if(!FORCE)
		{
			if(this.screenState==SCREEN_STATE_CALLING && screenState==SCREEN_STATE_LIST)
				return;
			
			this.screenState = screenState;
		}
		else
			this.screenState = screenState;
	}


	public final Handler handler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		if(msg.what==MSG_NUMBER)
    		{
    			String addString = (String) msg.obj;
    			
    			if(phoneNumberString.length()>=11)
    				phoneNumberString = "";
    			
    			phoneNumberString += addString;
    			setPhoneNumberTextView();
    			
    		}
    		else if(msg.what==MSG_BACKSPACE)
    		{
    			if(phoneNumberString.length()>0)
    			{
	    			phoneNumberString = phoneNumberString.substring(0, phoneNumberString.length()-1);
	    			setPhoneNumberTextView();
    			}
    		}
    		
    		else if(msg.what==MSG_CALLTO)
    		{
    			String phoneNumber = (String) msg.obj;
    			Session.getInstance().callTo(phoneNumber);
    			
    			/*
    			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    			transaction.replace(R.id.fragment_container, callingFragment);
    			transaction.addToBackStack(null);
    			transaction.commit();
    			*/
    		}
    		
    		else if(msg.what==MSG_CLOSECALLINGFRAGMENT)
    		{
    			getSupportFragmentManager().popBackStack();
    			handler.sendEmptyMessage(MSG_STOPCALLING);
    			
    		}
    		
    		else if(msg.what==MSG_CALLEDFROM)
    		{
    			//Log.i("scpark", "called from");
    			if(AdAndPhoneLayout.getInstance()!=null)
    				AdAndPhoneLayout.getInstance().handler.sendEmptyMessage(AdAndPhoneLayout.MSG_HIDEPHONELAYOUT);
    			
    			
    			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    			callingFragment.firstOpen(true);
    			callingFragment.setMode(CallingFragment.MODE_RECEIVE);
    			callingFragment.setDestSessionInfo((SessionInfo) msg.obj);
    			transaction.replace(R.id.fragment_container, callingFragment);
    			transaction.addToBackStack(null);
    			transaction.commit();
    			
    			handler.sendEmptyMessage(MainActivity.MSG_PLAYCALLING);
    			
    		}
    		
    		
    		else if(msg.what==MSG_CALLACCEPT)
    		{
    			
    		}
    		
    		else if(msg.what==MSG_CALLRESPONSE)
    		{
    			if(AdAndPhoneLayout.getInstance()!=null)
    				AdAndPhoneLayout.getInstance().handler.sendEmptyMessage(AdAndPhoneLayout.MSG_HIDEPHONELAYOUT);
    			
    			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    			callingFragment.firstOpen(true);
    			callingFragment.setMode(CallingFragment.MODE_SEND);
    			callingFragment.setDestSessionInfo((SessionInfo) msg.obj);
    			transaction.replace(R.id.fragment_container, callingFragment);
    			transaction.addToBackStack(null);
    			transaction.commit();
    			
    			handler.sendEmptyMessage(MainActivity.MSG_PLAYCALLING);
    		}
    		
    		else if(msg.what==MSG_CALLOFFED)
    		{
    			callingFragment.destroy();
    			getSupportFragmentManager().popBackStack();
    			handler.sendEmptyMessage(MSG_STOPCALLING);
    		}
    		
    		
    		else if(msg.what==MSG_SETPHONENUMBER)
    		{
    			phoneNumberString = ((String) msg.obj).replace("-", "");
    			setPhoneNumberTextView();
    		}
    		
    		else if(msg.what==MSG_OPENMODIFYPROFILE)
    		{
    			modifyProfileFragment = new ModifyProfileFragment();
				modifyProfileFragment.show(fragmentManager, "modifyProfileFragment");
    		}
    		
    		else if(msg.what==MSG_OPENINFO)
    		{
    			infoFragment = new PranktokInfoFragment();
				infoFragment.show(fragmentManager, "infoFragment");
    		}
    		
    		else if(msg.what==MSG_PLAYCALLING)
    		{
    			Resources res = getResources();
    			
    			if(mediaPlayer==null)
    			{
    				mediaPlayer = new MediaPlayer();
    				mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
    				AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.melodycafe); 
    				try {
    					mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
    				} catch (IllegalArgumentException e1) {
    					// TODO Auto-generated catch block
    					//e1.printStackTrace();
    				} catch (IllegalStateException e1) {
    					// TODO Auto-generated catch block
    					//e1.printStackTrace();
    				} catch (IOException e1) {
    					// TODO Auto-generated catch block
    					//e1.printStackTrace();
    				}
    				try {
    					mediaPlayer.prepare();
    				} catch (IllegalStateException e) {
    					// TODO Auto-generated catch block
    					//e.printStackTrace();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					//e.printStackTrace();
    				}
    			}
    		    if(mediaPlayer!=null)
    		    {
    		    	mediaPlayer.seekTo(0);
    		    	mediaPlayer.start();
    		    }
    		}
    		
    		else if(msg.what==MSG_STOPCALLING)
    		{
    			if(mediaPlayer!=null)
    				mediaPlayer.pause();
    			
    		}
    		
    		else if(msg.what==MSG_SPEAKERPHONEOFF)
    		{
    			audioManager.setSpeakerphoneOn(false);
    			AudioRecorder.getInstance().setAECenable(false);
    		}
    		
    		else if(msg.what==MSG_SPEAKERPHONEON)
    		{
    			audioManager.setSpeakerphoneOn(true);
    			AudioRecorder.getInstance().setAECenable(true);
    		}
    		else if(msg.what==MSG_WRONG_NUMBER_ERROR)
    		{
    			Toast toast = Toast.makeText(getApplicationContext(),
			   R.string.wrongnumber, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
    		}
    		else if(msg.what==MSG_DEST_CALLING_ERROR)
    		{
    			Toast toast = Toast.makeText(getApplicationContext(),
			   R.string.destcalling, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
    		}
    	}
    };
		
    private void setPhoneNumberTextView()
    {
    	String outputString = "";
		if(phoneNumberString.length()>0)
		{
			int remained;
			if(phoneNumberString.length()>=3)
				remained = 3;
			else
				remained = phoneNumberString.length();
			outputString = phoneNumberString.substring(0, remained);
		}
		
		if(phoneNumberString.length()>3)
		{
			outputString += "-";
			
			int remained;
			if(phoneNumberString.length()>=7)
				remained = 4;
			else
				remained = phoneNumberString.length() - 3;
			
			outputString += phoneNumberString.substring(3, 3 + remained);
		}
		
		if(phoneNumberString.length()>7)
			outputString += "-" + phoneNumberString.substring(7, phoneNumberString.length());
		
		phoneNumberTV.setText(outputString);
		
		////
		
		Message message = new Message();
		message.what = MainFragment.MSG_SEARCHPHONENUMBER;
		message.obj = phoneNumberString;
		MainFragment.getInstance().handler.sendMessage(message);
    }


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(mediaPlayer!=null)
		{
			mediaPlayer.stop();
			mediaPlayer.release();
		}
		
		if(soundPoolInited)
		{
			soundPool.unload(startSoundID);
			soundPool.unload(endSoundID);
			soundPool.release();
			soundPoolInited = false;
		}
		super.onDestroy();
	}


	



}
