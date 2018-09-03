package main;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

public class Buffer {
	public int key;
	public long time;
	//private LinkedList<String> tagList;
	public ByteBuffer byteBuffer;
	
	public Buffer(int key, ByteBuffer byteBuffer)
	{
		//tagList = new LinkedList<String>();
		this.key = key;
		this.byteBuffer = byteBuffer;
	}
	
	/*
	public void addTag(String string)
	{
		this.tagList.add(string);
	}
	*/
	/*
	public String getTags()
	{
		StringBuilder sb = new StringBuilder(1024);
		for(String string:tagList)
			sb.append(string);
		
		return sb.toString();
	}
	*/
}
