package com.scpark.prankcallclient;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class AdAndPhoneLayout extends LinearLayout {
	static AdAndPhoneLayout instance;
	
	MainActivity mainActivity = null;
	boolean mIsScrolling;
	int mTouchSlop;
	float yPrev;
	
	float bottomMarginInitValue;
	float bottomMargin;
	boolean bottomMarginValueInited = false;
	boolean ENABLE_LAYOUTMOVE = true;
	
	protected static final int MSG_MOVEPHONELAYOUT = 0;
	public static final int MSG_SHOWPHONELAYOUT = 1;
	public static final int MSG_HIDEPHONELAYOUT = 2;
	
	static AdAndPhoneLayout getInstance()
	{
		return instance;
	}

	public AdAndPhoneLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public AdAndPhoneLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public AdAndPhoneLayout(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		
		if(!bottomMarginValueInited)
		{
			bottomMarginInitValue = ((RelativeLayout.LayoutParams) this.getLayoutParams()).bottomMargin;
			bottomMargin = bottomMarginInitValue;
			bottomMarginValueInited = true;
		}
	}

	public void setMainActivity(MainActivity mainActivity)
	{
		this.mainActivity = mainActivity;
	}
	
	private void init(Context context)
	{
		instance = this;
		ViewConfiguration vc = ViewConfiguration.get(context);
		mTouchSlop = vc.getScaledTouchSlop();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		
			final int action = MotionEventCompat.getActionMasked(ev);
			
			switch(action) {
			case MotionEvent.ACTION_MOVE:
				//Log.i("action", "intercept move");
				float y = ev.getRawY();
				float diffY = yPrev - y;
				yPrev = y;
				
				Message msg = new Message();
				msg.what = MSG_MOVEPHONELAYOUT;
				msg.obj = diffY;
				handler.sendMessage(msg);
				return false;
				
			case MotionEvent.ACTION_DOWN:
				//Log.i("action", "intercept down");
				yPrev = (int) ev.getRawY();
				return false;
				
			case MotionEvent.ACTION_UP:
				//Log.i("action", "intercept up");
				magneticMove();
				return false;
			}
	
		return false;
	}

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
		
		final int action = MotionEventCompat.getActionMasked(ev);
		
		switch(action) {
		case MotionEvent.ACTION_MOVE:
			//Log.i("action", "move");
			float y = ev.getRawY();
			float diffY = yPrev - y;
			yPrev = y;
			
			Message msg = new Message();
			msg.what = MSG_MOVEPHONELAYOUT;
			msg.obj = diffY;
			handler.sendMessage(msg);
			
			return true;
			
		case MotionEvent.ACTION_DOWN:
			//Log.i("action", "down");
			return true;
			
		case MotionEvent.ACTION_UP:
			//Log.i("action", "up");
			magneticMove();
			return true;
		}

	return true;
    }
	
	private void magneticMove()
	{
		if(!this.ENABLE_LAYOUTMOVE)
			return;
		
		if(bottomMargin < bottomMarginInitValue/2)
		{
			handler.sendEmptyMessage(MSG_HIDEPHONELAYOUT);
		}
		else
		{
			handler.sendEmptyMessage(MSG_SHOWPHONELAYOUT);
			
		}
	}

	public final Handler handler = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		if(msg.what==MSG_MOVEPHONELAYOUT)
    		{
    			if(!ENABLE_LAYOUTMOVE)
    			{
    				return;
    			}
    			
    			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
    			//float bottomMargin = layoutParams.bottomMargin;
    			float newBottomMargin = bottomMargin + (Float) msg.obj;
    			
    			if(newBottomMargin < bottomMarginInitValue || newBottomMargin > 0)
    				return;
    			
    			bottomMargin = newBottomMargin;
    			layoutParams.setMargins(0, 0, 0, (int)bottomMargin);
    			setLayoutParams(layoutParams);
    			invalidate();
    			
    			float alpha = bottomMargin / bottomMarginInitValue;
    			Message message = new Message();
    			message.what = MainFragment.MSG_SETPHONEBUTTONALPHA;
    			message.obj = alpha;
    			
    			if(MainFragment.getInstance()!=null)
    				MainFragment.getInstance().handler.sendMessage(message);
    		}
    		
    		else if(msg.what==MSG_HIDEPHONELAYOUT)
    		{
    			ENABLE_LAYOUTMOVE = false;
    			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
    			//float bottomMargin = layoutParams.bottomMargin;
    			float newBottomMargin;
    			
    			if(bottomMargin - 100 <= bottomMarginInitValue)
    				newBottomMargin = bottomMarginInitValue;
    			else
    				newBottomMargin = bottomMargin - 100;
    			
    			if(bottomMargin <= bottomMarginInitValue)
    			{
    				if(MainFragment.getInstance()!=null)
    					MainFragment.getInstance().handler.sendEmptyMessage(MainFragment.MSG_SHOWPHONEBUTTON);
    				
    				MainActivity.getInstance().setScreenState(MainActivity.SCREEN_STATE_LIST, false);
    				if(MainActivity.getInstance().screenState!=MainActivity.SCREEN_STATE_CALLING)
    					ENABLE_LAYOUTMOVE = true;
    				return;
    			}
    			
    			bottomMargin = newBottomMargin;
    			layoutParams.setMargins(0, 0, 0, (int)bottomMargin);
    			setLayoutParams(layoutParams);
    			invalidate();
    			
    			try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
    			handler.sendEmptyMessage(MSG_HIDEPHONELAYOUT);
    			
    			msg = new Message();
    			msg.what = MainActivity.MSG_SETPHONENUMBER;
    			msg.obj = (String) "";
    			MainActivity.getInstance().handler.sendMessage(msg);
    			
    			float alpha = bottomMargin / bottomMarginInitValue;
    			Message message = new Message();
    			message.what = MainFragment.MSG_SETPHONEBUTTONALPHA;
    			message.obj = alpha;
    			
    			if(MainFragment.getInstance()!=null)
    				MainFragment.getInstance().handler.sendMessage(message);
    		}
    		
    		else if(msg.what==MSG_SHOWPHONELAYOUT)
    		{
    			ENABLE_LAYOUTMOVE = false;
    			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
    			//float bottomMargin = layoutParams.bottomMargin;
    			float newBottomMargin;
    			
    			if(bottomMargin + 100 >= 0)
    				newBottomMargin = 0;
    			else
    				newBottomMargin = bottomMargin + 100;
    			
    			if(bottomMargin >= 0)
    			{
    				if(MainFragment.getInstance()!=null)
    					MainFragment.getInstance().handler.sendEmptyMessage(MainFragment.MSG_HIDEPHONEBUTTON);
    				
    				MainActivity.getInstance().setScreenState(MainActivity.SCREEN_STATE_PHONE, false);
    				if(MainActivity.getInstance().screenState!=MainActivity.SCREEN_STATE_CALLING)
    					ENABLE_LAYOUTMOVE = true;
    				return;
    			}
    			
    			bottomMargin = newBottomMargin;
    			layoutParams.setMargins(0, 0, 0, (int)bottomMargin);
    			setLayoutParams(layoutParams);
    			invalidate();
    			
    			try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
    			handler.sendEmptyMessage(MSG_SHOWPHONELAYOUT);
    			
    			float alpha = bottomMargin / bottomMarginInitValue;
    			Message message = new Message();
    			message.what = MainFragment.MSG_SETPHONEBUTTONALPHA;
    			message.obj = alpha;
    			
    			if(MainFragment.getInstance()!=null)
    				MainFragment.getInstance().handler.sendMessage(message);
    		}
    		
    	}
	};
	
	public void setLayoutMoveEnable(boolean enable)
	{
		this.ENABLE_LAYOUTMOVE = enable;
	}
}
