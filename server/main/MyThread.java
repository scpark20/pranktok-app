package main;

public class MyThread extends Thread {
	protected int state;
	
	public final static int STATE_RUNNING = 0;
	public final static int STATE_STOPPED = 1;
}
