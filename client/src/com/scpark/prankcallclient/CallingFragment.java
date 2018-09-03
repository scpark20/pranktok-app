package com.scpark.prankcallclient;

import session.Session;
import session.SessionInfo;
import voice.AudioPlayer;
import voice.AudioRecorder;
import voice.OpenSLAudioPlayer;
import voice.PhaseVocoder;

import voice.VoiceEncoder;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import network.VoiceUDPManager;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class CallingFragment extends Fragment {
	static private CallingFragment instance = null;
	
	private static TextView emojiTV;
	private static TextView nickNameTV;
	private static TextView phoneNumberTV;
	private static TextView heartTV;
	private static TextView countryTV;
	
	private static ImageView callingBackgroundIV;
	private static ImageView selectPhoneIV;
	private static ViewGroup callingControlLayout;
	private static ViewGroup callingSettingLayout;
	private static ViewGroup callingAcceptLayout;
	private static Button calloffButton;
	private static ViewGroup clickmeLayout;
	
	private static ImageView callAcceptGradIV;
	private static ImageView callDenyGradIV;
	private static SeekBar pitchRateBar;
	private static Switch speakerSwitch;
	private static Switch myVoiceSwitch;
	
	private static Animation vibAnimation;
	private static Animation alphaAnimation;
	private static Animation bubbleGrowAnimation;
	private static boolean vibrated = false;
	private static boolean buddy;
	
	public static final int MSG_CALLACCEPTED = 0;
	public static final int MSG_CALLACCEPT = 1;
	//public static final int MSG_CALLDENIED = 1;
	public static final int MSG_CALLOFFED = 2;
	protected static final int MSG_HIDECALLINGACCEPTLAYOUT = 3;
	
	public static final int MODE_SEND = 0;
	public static final int MODE_RECEIVE = 1;

	private static final String PREF_PITCHRATE = "fjwengjn32t";
	private static final float PREF_PITCHRATE_DEFAULT = 1.0f;

	private static final int PITCH_PROGRESS_MAX = 10000;
	
	
	
	private static int callingMode = MODE_SEND;
	private static SessionInfo destSessionInfo;
	
	private static Bitmap bitmap = null;
	private boolean firstOpened;
	
	public CallingFragment()
	{
		//Log.i("scpark", "CallingFragment");
	}
	
	public CallingFragment(Bitmap bitmap) {
		// TODO Auto-generated constructor stub
		this.bitmap = bitmap;
	}
	
	public void setMode(int mode)
	{
		callingMode = mode;
	}
	
	public void setDestSessionInfo(SessionInfo destSessionInfo)
	{
		this.destSessionInfo = destSessionInfo;
	}


	static public CallingFragment getInstance()
	{
		return instance;
	}
	

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		
	}


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		instance = this;
		
		//Log.i("scpark", "create callingfragment");
	}

	@Override
	public void onAttach(Context context) {
		// TODO Auto-generated method stub
		super.onAttach(context);
		
	}




	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
	}

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		//Log.i("scpark", "onCreateView callingfragment mode : " + this.callingMode);
		MainActivity.getInstance().setScreenState(MainActivity.SCREEN_STATE_CALLING, false);
		
		View rootView = inflater.inflate(R.layout.activity_calling, container, false);
		
		
		callingAcceptLayout = (ViewGroup) rootView.findViewById(R.id.callingAcceptLayout);
		calloffButton = (Button) rootView.findViewById(R.id.callOffButton);
		calloffButton.setVisibility(View.INVISIBLE);
		
		calloffButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Message message = new Message();
    			message.what = MainActivity.MSG_CLOSECALLINGFRAGMENT;
    			message.obj = this;
    			MainActivity.getInstance().handler.sendMessage(message);
    			
    			destroy();
			}
			
		});
		
		this.emojiTV = (TextView) rootView.findViewById(R.id.destEmoji);
		this.nickNameTV = (TextView) rootView.findViewById(R.id.destNickName);
		this.phoneNumberTV = (TextView) rootView.findViewById(R.id.destPhoneNumber);
		this.heartTV = (TextView) rootView.findViewById(R.id.calling_heartFlag);
		this.countryTV = (TextView) rootView.findViewById(R.id.calling_countryFlag);
		
		int[] countryflagCode = Session.getInstance().getFlagCode(this.destSessionInfo.country);
		if(countryflagCode!=null)
			countryTV.setText(Utils.getEmojiByUnicode(countryflagCode));
		
		heartTV.setText(Utils.getEmojiByUnicode(0x1F496));
		
		if(Session.getInstance().buddiesDB.select(this.destSessionInfo.phoneNumber)!=null)
		{
			heartTV.setVisibility(View.VISIBLE);
			buddy = true;
		}
		else
		{
			heartTV.setVisibility(View.INVISIBLE);
			buddy = false;
		}
		
		clickmeLayout = (ViewGroup) rootView.findViewById(R.id.clickmeview);
		bubbleGrowAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.bubblegrow);
		if(!buddy)
		{
			clickmeLayout.setVisibility(View.VISIBLE);
			clickmeLayout.startAnimation(bubbleGrowAnimation);
		}
		else
		{
			clickmeLayout.setVisibility(View.INVISIBLE);
			clickmeLayout.clearAnimation();
		}
		
		int emojiCode = this.destSessionInfo.emojiCode;
		emojiTV.setText(Utils.getEmojiByUnicode(emojiCode));
		emojiTV.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(buddy)
				{
					buddy = false;
					heartTV.setVisibility(View.INVISIBLE);
					Session.getInstance().buddiesDB.delete(destSessionInfo);
					
					Toast toast = Toast.makeText(getContext(),
							   R.string.notlovelyfriend, Toast.LENGTH_SHORT);
								toast.setGravity(Gravity.CENTER, 0, 0);
								toast.show();
								
					clickmeLayout.setVisibility(View.VISIBLE);
					clickmeLayout.startAnimation(bubbleGrowAnimation);			
				}
				else
				{
					buddy = true;
					heartTV.setVisibility(View.VISIBLE);
					Session.getInstance().buddiesDB.write(destSessionInfo);
					
					Toast toast = Toast.makeText(getContext(),
							R.string.lovelyfriend, Toast.LENGTH_SHORT);
								toast.setGravity(Gravity.CENTER, 0, 0);
								toast.show();
								
					clickmeLayout.setVisibility(View.INVISIBLE);
					clickmeLayout.clearAnimation();			
				}
			}
			
		});
		
		nickNameTV.setText(destSessionInfo.nickName);
		phoneNumberTV.setText(Utils.phoneNumberFormatter(destSessionInfo.phoneNumber));
		
		selectPhoneIV = (ImageView) rootView.findViewById(R.id.selectPhone);
		
		vibAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.vibration);
		selectPhoneIV.startAnimation(vibAnimation);
		
		selectPhoneIV.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				int action = event.getAction();
				
				switch(action)
				{
					case MotionEvent.ACTION_DOWN:
						View.DragShadowBuilder myShadow = new View.DragShadowBuilder(v);
						v.startDrag(null, myShadow, null, 0);
						v.clearAnimation();
						v.setVisibility(View.INVISIBLE);
						return false;
					
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						//v.setVisibility(View.VISIBLE);
						return false;
					default:
						return false;
				}
			}
			
		});
		
		
		
		callingControlLayout = (ViewGroup) rootView.findViewById(R.id.callingControlLayout);
		callingControlLayout.setOnDragListener(new OnDragListener(){

			@Override
			public boolean onDrag(View v, DragEvent event) {
				// TODO Auto-generated method stub
				int action = event.getAction();
				
				switch(action) {
					case DragEvent.ACTION_DRAG_LOCATION:
						//LOG.I("DRAG_LOCATION");
						if(event.getX() > v.getWidth() * 2.0f/3.0f)
						{
							if(!vibrated)
							{
								Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
								vibrator.vibrate(30);
							}
							vibrated = true;
						}
						else
							vibrated = false;
						
						if(callingMode==MODE_RECEIVE)
						{
							if(event.getX() < v.getWidth() * 1.0f/3.0f)
							{
								if(!vibrated)
								{
									Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
									vibrator.vibrate(30);
								}
								vibrated = true;
							}
							else
								vibrated = false;
						}
						
						break;
					case DragEvent.ACTION_DRAG_STARTED:
						//LOG.I("DRAG_STARTED");
						break;
					case DragEvent.ACTION_DRAG_ENTERED:
						//LOG.I("DRAG_ENTERED");
						break;
					case DragEvent.ACTION_DRAG_EXITED:
						//LOG.I("DRAG_EXITED");
						selectPhoneIV.setVisibility(View.VISIBLE);
						selectPhoneIV.startAnimation(vibAnimation);
						break;
						
					case DragEvent.ACTION_DRAG_ENDED:
						//LOG.I("DRAG_ENDED");
						break;
					case DragEvent.ACTION_DROP:
						//LOG.I("DRAG_DROP");
						selectPhoneIV.setVisibility(View.VISIBLE);
						selectPhoneIV.startAnimation(vibAnimation);
						
						if(event.getX() > v.getWidth() * 2.0f/3.0f)
						{
							Message msg = new Message();
							msg.what = MainActivity.MSG_CLOSECALLINGFRAGMENT;
							msg.obj = instance; 
							MainActivity.getInstance().handler.sendMessage(msg);
							destroy();
							
						}
						
						if(callingMode == MODE_RECEIVE)
							if(event.getX() < v.getWidth() * 1.0f/3.0f)
							{
								handler.sendEmptyMessage(MSG_CALLACCEPT);
								Session.getInstance().callAccept();
							}
							
						break;
					
				}
				return true;
			}
			
		});
		
		callingSettingLayout = (ViewGroup) rootView.findViewById(R.id.callingSettingLayout);
		callingSettingLayout.setVisibility(View.INVISIBLE);
		
		callAcceptGradIV = (ImageView) rootView.findViewById(R.id.callacceptgrad);
		callDenyGradIV = (ImageView) rootView.findViewById(R.id.calldenygrad);
		
		alphaAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.alpha);
		
		
		
		if(callingMode == MODE_SEND)
		{
			callAcceptGradIV.setVisibility(View.INVISIBLE);
			callDenyGradIV.setVisibility(View.VISIBLE);
			callDenyGradIV.startAnimation(alphaAnimation);
		}
		else
		{
			callAcceptGradIV.setVisibility(View.VISIBLE);
			callAcceptGradIV.startAnimation(alphaAnimation);
			callDenyGradIV.setVisibility(View.VISIBLE);
			callDenyGradIV.startAnimation(alphaAnimation);
		}
			
		
		callingBackgroundIV = (ImageView) rootView.findViewById(R.id.callingBackgroundImage);
		if(bitmap != null)
			callingBackgroundIV.setImageBitmap(bitmap);
		 
		pitchRateBar = (SeekBar) rootView.findViewById(R.id.pitchRateBar);
		
		SharedPreferences sharedPref = this.getContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
		float pitchRate = sharedPref.getFloat(PREF_PITCHRATE, PREF_PITCHRATE_DEFAULT);
		PhaseVocoder.getInstance().setPitchrate(pitchRate);
		
		pitchRateBar.setMax(PITCH_PROGRESS_MAX);
		pitchRateBar.setProgress(pitchRate2progress(pitchRate));
		
		pitchRateBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				float pitchRate = progress2pitchRate(progress);
				PhaseVocoder.getInstance().setPitchrate(pitchRate);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				float pitchRate = progress2pitchRate(seekBar.getProgress());
				
				SharedPreferences sharedPref = MainActivity.getInstance().getApplicationContext().getSharedPreferences(C.PREF_FILE_KEY, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putFloat(PREF_PITCHRATE, pitchRate);
				editor.commit();
			}
			
		});
		
		speakerSwitch = (Switch) rootView.findViewById(R.id.speakerToggle);
		speakerSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked)
					MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_SPEAKERPHONEON);
				else
					MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_SPEAKERPHONEOFF);
			}
			
		});
		
		myVoiceSwitch = (Switch) rootView.findViewById(R.id.myVoiceToggle);
		myVoiceSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked)
					AudioPlayer.getInstance().startPlay();
				else
					AudioPlayer.getInstance().stopPlay();
			}
			
		});
		
		if(!firstOpened)
			handler.sendEmptyMessage(MSG_HIDECALLINGACCEPTLAYOUT);
		
		firstOpened = false;
		
		return rootView;
	}
	
	private int pitchRate2progress(float pitchRate)
	{
		if(pitchRate>=1.0f)
			return (int) (PITCH_PROGRESS_MAX / 2.0f * pitchRate);
		else
			return (int) ((pitchRate - 0.5f) * PITCH_PROGRESS_MAX);
	}
	
	private float progress2pitchRate(int progress)
	{
		if(progress >= PITCH_PROGRESS_MAX)
			return 2.0f / (float)PITCH_PROGRESS_MAX * progress;
		else
			return 1.0f / (float)PITCH_PROGRESS_MAX * progress + 0.5f;
	}
	
	public void destroy()
	{
		MainActivity.getInstance().setScreenState(MainActivity.SCREEN_STATE_LIST, true);
		
		Session.getInstance().callOff();
		
		AudioRecorder.getInstance().stopRecord();
		OpenSLAudioPlayer.getInstance().stop();
		AudioPlayer.getInstance().stopPlay();
		
		VoiceEncoder.getInstance().stopEncoding();
		
		Session.getInstance().stopTransmission();
		Session.getInstance().voiceInputQueueReset();
		
		if(MainActivity.getInstance().soundPool!=null)
			MainActivity.getInstance().soundPool.play(MainActivity.endSoundID, 0.8f, 0.8f, 0, 0, 1.0f);
		
		AdAndPhoneLayout.getInstance().setLayoutMoveEnable(true);
		instance = null;	
	}
	
	public void firstOpen(boolean firstOpen)
	{
		this.firstOpened = firstOpen;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	public final Handler handler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		if(msg.what==MSG_HIDECALLINGACCEPTLAYOUT)
    		{
    			callingSettingLayout.setVisibility(View.VISIBLE);
    			callingAcceptLayout.setVisibility(View.INVISIBLE);
    			calloffButton.setVisibility(View.VISIBLE);
    			
    		}
    		if(msg.what==MSG_CALLACCEPTED)
    		{
    			callingSettingLayout.setVisibility(View.VISIBLE);
    			callingAcceptLayout.setVisibility(View.INVISIBLE);
    			
    			AudioRecorder.getInstance().startRecord();
    			OpenSLAudioPlayer.getInstance().start();
    			//AudioPlayer.getInstance().startPlay();
    			
    			VoiceEncoder.getInstance().startEncoding();
    			
    			calloffButton.setVisibility(View.VISIBLE);
    			
    			MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_STOPCALLING);
    			MainActivity.getInstance().soundPool.play(MainActivity.startSoundID, 0.8f, 0.8f, 0, 0, 1.0f);
    		}
    		
    		else if(msg.what==MSG_CALLACCEPT)
    		{
    			callingSettingLayout.setVisibility(View.VISIBLE);
    			callingAcceptLayout.setVisibility(View.INVISIBLE);
    			
    			AudioRecorder.getInstance().startRecord();
    			OpenSLAudioPlayer.getInstance().start();
    			//AudioPlayer.getInstance().startPlay();
    			
    			VoiceEncoder.getInstance().startEncoding();
    			
    			calloffButton.setVisibility(View.VISIBLE);
    			
    			MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_STOPCALLING);
    			MainActivity.getInstance().soundPool.play(MainActivity.startSoundID, 0.8f, 0.8f, 0, 0, 1.0f);
    		}
    		
    		else if(msg.what==MSG_CALLOFFED)
    		{
    			Message message = new Message();
    			message.what = MainActivity.MSG_CLOSECALLINGFRAGMENT;
    			message.obj = this;
    			MainActivity.getInstance().handler.sendMessage(message);
    			destroy();
    		}
    	}
	};
	
}

