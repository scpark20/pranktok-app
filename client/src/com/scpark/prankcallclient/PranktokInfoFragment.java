package com.scpark.prankcallclient;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class PranktokInfoFragment extends DialogFragment {
	
	WebView infoWebView;

	@Override
	@Nullable
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.infofragment, container, false);
		infoWebView = (WebView) rootView.findViewById(R.id.infoWebView);
		infoWebView.loadUrl("file:///android_res/raw/info.html");
		return rootView;
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}

}


