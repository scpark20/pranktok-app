package com.scpark.prankcallclient;

import java.util.ArrayList;


import session.Session;
import session.SessionInfo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;

public class ModifyProfileFragment extends DialogFragment {
	static private ModifyProfileFragment instance = null;
	
	
	
	Spinner myFaceSpinner;
	EditText myNickNameET;
	EditText myPhoneNumberET;
	
	ImageButton refreshNickNameIB;
	ImageButton refreshPhoneNumberIB;
	
	Switch myCountryOnlySwitch;

	private Animation rotateAnimation;
	
	static public final int MSG_SETPHONENUMBER = 0;
	static public final int MSG_SETNICKNAME = 1;
	
	static public ModifyProfileFragment getInstance()
	{
		return instance;
	}

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.modifyprofile, container, false);
		
		instance = this;
		
		
		
		FaceArrayAdapter faceArrayAdapter = new FaceArrayAdapter(this.getContext(), R.layout.face_spinner_item, Session.getInstance().faceItemList);
		myFaceSpinner = (Spinner) rootView.findViewById(R.id.myFaceSpinner);
		myFaceSpinner.setAdapter(faceArrayAdapter);
		
		int position = Session.getInstance().getEmojiPosition(Session.getInstance().getEmojiCode());
		myFaceSpinner.setSelection(position);
		
		this.myNickNameET = (EditText) rootView.findViewById(R.id.myNickNameET);
		myNickNameET.setText(Session.getInstance().getNickName());
		
		this.myPhoneNumberET = (EditText) rootView.findViewById(R.id.myPhoneNumberET);
		myPhoneNumberET.setText(Utils.phoneNumberFormatter(Session.getInstance().getPhoneNumber()));
		
		this.refreshNickNameIB = (ImageButton) rootView.findViewById(R.id.refreshNickName);
		refreshNickNameIB.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(Session.getInstance()!=null)
					Session.getInstance().getNewNickName();
				
				refreshNickNameIB.startAnimation(rotateAnimation);
			}
			
		});
		
		this.refreshPhoneNumberIB = (ImageButton) rootView.findViewById(R.id.refreshPhoneNumber);
		refreshPhoneNumberIB.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(Session.getInstance()!=null)
					Session.getInstance().getNewPhoneNumber();
				
				refreshPhoneNumberIB.startAnimation(rotateAnimation);
			}
			
		});
		
		rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
		
		myCountryOnlySwitch = (Switch) rootView.findViewById(R.id.myCountrySwitch);
		myCountryOnlySwitch.setChecked(Session.getInstance().myCountryOnly);
		myCountryOnlySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				Session.getInstance().setMyCountryOnly(isChecked);
			}
			
		});
		return rootView;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		
		instance = null;
		
		int emojiCode = Session.getInstance().getEmojiCodeByPosition(myFaceSpinner.getSelectedItemPosition());
		String myNickName = myNickNameET.getText().toString();
		
		if(Session.getInstance()!=null)
		{
			Session.getInstance().putEmojiCode(emojiCode);
			Session.getInstance().putNickName(myNickName);
		}
		
		super.onDestroy();
	}
	
	public final Handler handler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		
    		if(msg.what==MSG_SETPHONENUMBER)
    		{
    			myPhoneNumberET.setText(Utils.phoneNumberFormatter((String)msg.obj));
    		}
    		
    		else if(msg.what==MSG_SETNICKNAME)
    		{
    			myNickNameET.setText((String)msg.obj);
    		} 
    	}
	};
}
