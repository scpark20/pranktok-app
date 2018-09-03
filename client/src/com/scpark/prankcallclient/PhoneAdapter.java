package com.scpark.prankcallclient;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;


import session.Session;
import session.SessionInfo;
import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PhoneAdapter extends BaseAdapter {
	CopyOnWriteArrayList<SessionInfo> ownSessionInfoList;
	CopyOnWriteArrayList<SessionInfo> ownBuddiesSessionInfoList;
	CopyOnWriteArrayList<SessionInfo> showSessionInfoList;
	LayoutInflater layoutInflater;
	String searchPhoneNumber = "";
	Animation[] animations;
	Random random = new Random();

	public PhoneAdapter(Activity activity) {
		// TODO Auto-generated constructor stub
		super();
		this.ownSessionInfoList = new CopyOnWriteArrayList<SessionInfo>();
		this.showSessionInfoList = new CopyOnWriteArrayList<SessionInfo>();
		ownBuddiesSessionInfoList = new CopyOnWriteArrayList<SessionInfo>();
		layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		/*
		animations = new Animation[11];
		animations[0] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.vibration);
		animations[1] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.alpha);
		animations[2] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.grow);
		animations[3] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.shrink);
		animations[4] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.infiniterotate);
		animations[5] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.blink);
		animations[6] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.fade);
		animations[7] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.clockwise);
		animations[8] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.myanimation);
		animations[9] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.slide);
		animations[10] = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.move);
		*/
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return showSessionInfoList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return showSessionInfoList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	public void deleteItem(int position)
	{
		SessionInfo sessionInfo = showSessionInfoList.get(position);
		ownBuddiesSessionInfoList.remove(sessionInfo);
		showSessionInfoList.remove(sessionInfo);
		this.notifyDataSetInvalidated();
	}

	@Override
	public View getView(final int position, View view, ViewGroup parentView) {
		// TODO Auto-generated method stub
		
		if(view==null)
			view = layoutInflater.inflate(R.layout.phone_book_item, null);
		
		if(showSessionInfoList!=null && showSessionInfoList.size()>0)
		{
			SessionInfo sessionInfo = showSessionInfoList.get(position);
			TextView phoneBookItemNickname = (TextView) view.findViewById(R.id.phone_book_item_nickname);
			TextView phoneBookItemPhonenumber = (TextView) view.findViewById(R.id.phone_book_item_phonenumber);
			TextView phoneBookItemFace = (TextView) view.findViewById(R.id.phone_book_item_face);
			View veil = (View) view.findViewById(R.id.veil);
			TextView phoneBookItemHeart = (TextView) view.findViewById(R.id.phone_book_item_heartFlag);
			ImageView deleteButton = (ImageView) view.findViewById(R.id.deleteButton);
			deleteButton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Session.getInstance().buddiesDB.delete(showSessionInfoList.get(position));
					deleteItem(position);
				}
				
			});
			
			
			if(sessionInfo.buddy==true)
				phoneBookItemHeart.setText(Utils.getEmojiByUnicode(0x1F496));
			
			if(sessionInfo.connect==true)
				veil.setVisibility(View.INVISIBLE);
			else
				veil.setVisibility(View.VISIBLE);
			
			phoneBookItemNickname.setText(sessionInfo.nickName);
			phoneBookItemPhonenumber.setText(Utils.phoneNumberFormatter(sessionInfo.phoneNumber));
			phoneBookItemFace.setText(Utils.getEmojiByUnicode(sessionInfo.emojiCode));
			//phoneBookItemFace.startAnimation(animations[random.nextInt(animations.length)]);
			
			TextView flagTV = (TextView) view.findViewById(R.id.phone_book_item_flag);
			TextView flagStringTV = (TextView) view.findViewById(R.id.phone_book_item_flagString);
			
			String flagString;
			int[] flagCode = Session.getInstance().getFlagCode(sessionInfo.country);
			if(flagCode==null)
			{
				flagString = sessionInfo.country;
				flagStringTV.setText(flagString);
				flagStringTV.setVisibility(View.VISIBLE);
			}
			else
			{
				flagString = Utils.getEmojiByUnicode(flagCode);
				flagTV.setText(flagString);
			}
		}
		return view;
	}
	
	
	public void addItems(ArrayList<SessionInfo> sessionInfoList)
	{
		for(SessionInfo sessionInfo: sessionInfoList)
		{
			this.ownSessionInfoList.add(sessionInfo);
			/*
			if(sessionInfo.phoneNumber.contains(searchPhoneNumber))
				this.showSessionInfoList.add(sessionInfo);
			*/
		}
	}
	
	public void addItem(SessionInfo newSessionInfo)
	{
		for(SessionInfo sessionInfo: ownBuddiesSessionInfoList)
			if(sessionInfo.phoneNumber.equals(newSessionInfo.phoneNumber))
				return;
		
	
		
		this.ownSessionInfoList.add(newSessionInfo);
		/*
		if(sessionInfo.phoneNumber.contains(searchPhoneNumber))
			this.showSessionInfoList.add(sessionInfo);
		*/
	}
	
	public void addBuddyItem(SessionInfo newSessionInfo)
	{
		SessionInfo existSessionInfo = null;
		for(SessionInfo sessionInfo:this.ownBuddiesSessionInfoList)
		{
			if(sessionInfo.phoneNumber.equals(newSessionInfo.phoneNumber))
				existSessionInfo = sessionInfo;
		}
		if(existSessionInfo!=null)
			ownBuddiesSessionInfoList.remove(existSessionInfo);
		
		this.ownBuddiesSessionInfoList.add(newSessionInfo);
	}
	
	public void clearBuddies()
	{
		this.ownBuddiesSessionInfoList.clear();
		
	}

	@Override
	public void notifyDataSetChanged() {
		// TODO Auto-generated method stub
		showSessionInfoList.clear();
			if(searchPhoneNumber.length()==0)
			{
				
				showSessionInfoList.addAll(ownBuddiesSessionInfoList);
				showSessionInfoList.addAll(ownSessionInfoList);
			}
			else
			{
				for(SessionInfo sessionInfo: ownBuddiesSessionInfoList)
				{
					if(sessionInfo.phoneNumber.contains(searchPhoneNumber))
						showSessionInfoList.add(sessionInfo);
				}
				
				for(SessionInfo sessionInfo: ownSessionInfoList)
				{
					if(sessionInfo.phoneNumber.contains(searchPhoneNumber))
						showSessionInfoList.add(sessionInfo);
				}
			}
		
		super.notifyDataSetChanged();
	}

	public void searchPhoneNumber(String phoneNumber)
	{
		this.searchPhoneNumber = phoneNumber;
	}

	public void clear() {
		// TODO Auto-generated method stub
		this.ownSessionInfoList.clear();
	}
	
	public SessionInfo getListViewItem(int position)
	{
		return this.showSessionInfoList.get(position);
	}

	public void notifyBuddyNotExist(String phoneNumber) {
		// TODO Auto-generated method stub
		SessionInfo existSessionInfo = null;
		for(SessionInfo sessionInfo:this.ownBuddiesSessionInfoList)
		{
			if(sessionInfo.phoneNumber.equals(phoneNumber))
				existSessionInfo = sessionInfo;
		}
		
		if(existSessionInfo!=null)
			existSessionInfo.connect = false;
	}

}
