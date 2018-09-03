package main;

public class LOG {
	public static void I(String string)
	{
		System.out.println("INFO : " + string);
	}
	
	public static void E(String string)
	{
		System.err.println("ERROR : " + string);
	}
	
	public static void C(String string)
	{
		System.out.println("CONSOLE : " + string);
	}
}
