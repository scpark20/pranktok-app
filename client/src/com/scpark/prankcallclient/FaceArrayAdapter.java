package com.scpark.prankcallclient;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FaceArrayAdapter<T> extends ArrayAdapter<T> {
	int resource;
	Context context;
	ArrayList<Integer> items;
	
	public FaceArrayAdapter(Context context, int resource, List<T> items) {
		super(context, resource, items);
		// TODO Auto-generated constructor stub
		this.resource = resource;
		this.context = context;
		this.items = (ArrayList<Integer>) items;
	}


	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if(convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(resource, null);
		}
		
		TextView faceTV = (TextView) convertView.findViewById(R.id.facespinner_textview);
		faceTV.setText(Utils.getEmojiByUnicode((int) items.get(position)));
		return convertView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if(convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(resource, null);
		}
		
		TextView faceTV = (TextView) convertView.findViewById(R.id.facespinner_textview);
		faceTV.setText(Utils.getEmojiByUnicode((int) items.get(position)));
		return convertView;
	}

}
