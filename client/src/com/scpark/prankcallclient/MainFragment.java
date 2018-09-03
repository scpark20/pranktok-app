package com.scpark.prankcallclient;

import session.Session;
import session.SessionInfo;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import network.SessionManager;

public class MainFragment extends Fragment {
	static private MainFragment instance = null;
	
	static int myEmojiUnicode = 0;
	static String myPhoneNumber = null;
	
	static OnMainFragmentLisenter mCallback;
	
	static ListView listView;
	
	static TextView myNickNameTV;
	static TextView myPhoneNumberTV;
	static TextView myFaceTV;
	static TextView myFlagTV;
	static TextView myFlagStringTV;
	static ImageView refreshButton;
	static ImageView phoneButton;
	static ImageView pranktoklogoButton;
	
	static RelativeLayout myProfileLayout;
	
	static PhoneAdapter phoneAdapter;
	
	static Animation rotateAnimation;
	static Animation shrinkAnimation;
	static Animation growAnimation;
	
	public static final int MSG_SETFACE = 0;
	public static final int MSG_SETNICKNAME = 1;
	public static final int MSG_SETPHONENUMBER = 2;
	
	public static final int MSG_LISTREFRESH = 3;
	public static final int MSG_PUTUSERLIST = 4;
	public static final int MSG_PUTBUDDYUSERLIST = 5;
	public static final int MSG_PUTBUDDYUSERNONLIST = 6;

	public static final int MSG_LISTCLEAR = 7;
	public static final int MSG_LISTUPDATED = 8;

	protected static final int MSG_SHOWPHONEBUTTON = 9;
	protected static final int MSG_HIDEPHONEBUTTON = 10;
	
	public static final int MSG_SETPHONEBUTTONALPHA = 11;
	protected static final int MSG_SEARCHPHONENUMBER = 12;
	public static final int MSG_HIDEDELETEBUTTON = 13;

	public static final int MSG_HIDEREFRESHBUTTON = 14;
	public static final int MSG_SHOWREFRESHBUTTON = 15;
	
	
	
	static public MainFragment getInstance()
	{
		return instance;
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		instance = this;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		instance = null;
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Session.getInstance().getUserList(C.USER_LIST);
	}

	public interface OnMainFragmentLisenter {
		
	}

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.mainfragment, container, false);
		setListViewLayout(rootView);
		setMyProfile(rootView);
		setLogo(rootView);
		
		phoneButton = (ImageView) rootView.findViewById(R.id.phoneButton);
		phoneButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(AdAndPhoneLayout.getInstance()!=null)
					AdAndPhoneLayout.getInstance().handler.sendEmptyMessage(AdAndPhoneLayout.MSG_SHOWPHONELAYOUT);
				
				phoneButton.setVisibility(View.INVISIBLE);
			}
		});
		
		rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
		shrinkAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.shrink);
		growAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.grow);
		
		return rootView;
	}
	
	private void setLogo(View view)
	{
		pranktoklogoButton = (ImageView) view.findViewById(R.id.pranktoklogo);
		pranktoklogoButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				pranktoklogoButton.startAnimation(rotateAnimation);
				MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_OPENINFO);
			}
        	
        });
	}

	private void setMyProfile(View view) {
		// TODO Auto-generated method stub
		myFaceTV = (TextView) view.findViewById(R.id.myFace);
		myFaceTV.setText(Utils.getEmojiByUnicode(Session.getInstance().getEmojiCode()));
		
		
		myFlagTV = (TextView) view.findViewById(R.id.myFlag);
		myFlagStringTV = (TextView) view.findViewById(R.id.myFlagString);
		
		String flagString;
		int[] flagCode = Session.getInstance().getFlagCode();
		if(flagCode==null)
		{
			flagString = Session.getInstance().getCountry();
			myFlagStringTV.setText(flagString);
			myFlagStringTV.setVisibility(View.VISIBLE);
		}
		else
		{
			flagString = Utils.getEmojiByUnicode(flagCode);
			myFlagTV.setText(flagString);
		}	
		
		
		myNickNameTV = (TextView) view.findViewById(R.id.myNickName);
		myNickNameTV.setText(Session.getInstance().getNickName());
		 
		myPhoneNumberTV = (TextView) view.findViewById(R.id.myPhoneNumber);
		myPhoneNumberTV.setText(Utils.phoneNumberFormatter(Session.getInstance().getPhoneNumber()));
		
		myProfileLayout = (RelativeLayout) view.findViewById(R.id.myProfileLayout);
		
		myProfileLayout.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(MainActivity.getInstance()!=null)
					MainActivity.getInstance().handler.sendEmptyMessage(MainActivity.MSG_OPENMODIFYPROFILE);
			}
		});
	}

	

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnMainFragmentLisenter) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
	}

	private void setListViewLayout(View view)
	{
		 listView = (ListView) view.findViewById(R.id.phoneListView);
	        
        phoneAdapter = new PhoneAdapter(this.getActivity());
        
        SessionInfo[] sessionInfos = Session.getInstance().buddiesDB.selectAll();
        for(SessionInfo sessionInfo: sessionInfos)
        	phoneAdapter.addBuddyItem(sessionInfo);
        phoneAdapter.notifyDataSetChanged();
        listView.setAdapter(phoneAdapter);
        
        listView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MainActivity.MSG_SETPHONENUMBER;
				msg.obj = phoneAdapter.getListViewItem(position).phoneNumber;
				MainActivity.getInstance().handler.sendMessage(msg);
				
				
				if(AdAndPhoneLayout.getInstance()!=null)
					AdAndPhoneLayout.getInstance().handler.sendEmptyMessage(AdAndPhoneLayout.MSG_SHOWPHONELAYOUT);
			}
        	
        });
        
        listView.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				
				SessionInfo sessionInfo = (SessionInfo) phoneAdapter.getItem(position);
				if(sessionInfo.buddy)
				{
					view.findViewById(R.id.deleteButton).setVisibility(View.VISIBLE);
					view.findViewById(R.id.deleteButton).startAnimation(growAnimation);
					refreshButton.startAnimation(shrinkAnimation);
					refreshButton.setClickable(false);
					//refreshButton.setVisibility(View.INVISIBLE);
				}	
				return true;
			}
        	
        });
        
        refreshButton = (ImageView) view.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				handler.sendEmptyMessage(MSG_LISTREFRESH);
				refreshButton.startAnimation(rotateAnimation);
			}
        	
        });
	}
	
	public final Handler handler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		if(msg.what==MSG_SETFACE)
    		{
    			int faceUnicode = msg.arg1; 
    			myFaceTV.setText(Utils.getEmojiByUnicode(faceUnicode));
    		}
    		
    		else if(msg.what==MSG_SETNICKNAME)
    		{
    			myNickNameTV.setText((String)msg.obj); 
    		}
    		
    		else if(msg.what==MSG_SETPHONENUMBER)
    		{
    			myPhoneNumberTV.setText(Utils.phoneNumberFormatter((String)msg.obj));
    		}
    		
    		else if(msg.what==MSG_LISTREFRESH)
    		{
    			Session.getInstance().getUserList(C.USER_LIST);
    		}
    		
    		else if(msg.what==MSG_LISTCLEAR)
    		{
    			phoneAdapter.clear();
    		}
    		
    		else if(msg.what==MSG_PUTUSERLIST)
    		{
    			phoneAdapter.addItem((SessionInfo) msg.obj);
    		}
    		
    		else if(msg.what==MSG_PUTBUDDYUSERLIST)
    		{
    			phoneAdapter.addBuddyItem((SessionInfo) msg.obj);
    		}
    		
    		else if(msg.what==MSG_PUTBUDDYUSERNONLIST)
    		{
    			phoneAdapter.notifyBuddyNotExist((String) msg.obj);
    		}
    		
    		else if(msg.what==MSG_LISTUPDATED)
    		{
    			phoneAdapter.notifyDataSetChanged();
    			
    		}
    		
    		else if(msg.what==MSG_SHOWPHONEBUTTON)
    		{
    			phoneButton.setVisibility(View.VISIBLE);
    			refreshButton.setVisibility(View.VISIBLE);
    		}
    		
    		else if(msg.what==MSG_HIDEPHONEBUTTON)
    		{
    			phoneButton.setVisibility(View.INVISIBLE);
    			refreshButton.setVisibility(View.INVISIBLE);
    		}
    		else if(msg.what==MSG_SETPHONEBUTTONALPHA)
    		{
    			phoneButton.setAlpha((Float)msg.obj);
    			refreshButton.setAlpha((Float)msg.obj);
    		}
    		
    		else if(msg.what==MSG_SEARCHPHONENUMBER)
    		{
    			phoneAdapter.searchPhoneNumber((String)msg.obj);
    			phoneAdapter.notifyDataSetChanged();
    		}
    		
    		else if(msg.what==MSG_HIDEDELETEBUTTON)
    		{
    			int start = listView.getFirstVisiblePosition();
    			int last = start + listView.getChildCount();
    			
    			for(int i=start;i<last;i++)
    			{
    				View view = listView.getAdapter().getView(i, listView.getChildAt(i), listView);
    				view.findViewById(R.id.deleteButton).setVisibility(View.INVISIBLE);
    				view.findViewById(R.id.deleteButton).clearAnimation();
    			}
    			
    				refreshButton.clearAnimation();
    				refreshButton.setClickable(true);
    			
    		}
    		else if(msg.what==MSG_HIDEREFRESHBUTTON)
    		{
    		}
    		
    	}
	};
}
