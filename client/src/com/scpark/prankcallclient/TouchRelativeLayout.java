package com.scpark.prankcallclient;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.RelativeLayout;

public class TouchRelativeLayout extends RelativeLayout {

	public TouchRelativeLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}




	public TouchRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public TouchRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		if(ev.getAction()==MotionEvent.ACTION_DOWN)
			MainFragment.getInstance().handler.sendEmptyMessage(MainFragment.MSG_HIDEDELETEBUTTON);
		
		return false;
	}

}
