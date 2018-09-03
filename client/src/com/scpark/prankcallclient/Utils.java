package com.scpark.prankcallclient;

public class Utils {
	static public String phoneNumberFormatter(String rawPhoneNumber)
	{
		if(rawPhoneNumber.length()!=11)
			return rawPhoneNumber;
		
		return rawPhoneNumber.substring(0, 3) + "-" + rawPhoneNumber.substring(3, 7) + "-" + rawPhoneNumber.substring(7, 11);
	}
	
	static public String getEmojiByUnicode(int unicode){
	    return new String(Character.toChars(unicode));
	}
	
	static public String getEmojiByUnicode(int[] unicode){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<unicode.length;i++)
			sb.append(Character.toChars(unicode[i]));
		
	    return sb.toString();
	}
	
	static public int getSystemTimeInteger()
	{
		return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
	}
	
	static public long ipToLong(String ipAddress) {
		
		long result = 0;
			
		String[] ipAddressInArray = ipAddress.split("\\.");

		for (int i = 3; i >= 0; i--) {
				
			long ip = Long.parseLong(ipAddressInArray[3 - i]);
				
			//left shifting 24,16,8,0 and bitwise OR
				
			//1. 192 << 24
			//1. 168 << 16
			//1. 1   << 8
			//1. 2   << 0
			result |= ip << (i * 8);
			
		}

		return result;
	  }
	
	static public String longToIp(long ip) {
		StringBuilder sb = new StringBuilder(15);

		for (int i = 0; i < 4; i++) {

			// 1. 2
			// 2. 1
			// 3. 168
			// 4. 192
			sb.insert(0, Long.toString(ip & 0xff));

			if (i < 3) {
				sb.insert(0, '.');
			}

			// 1. 192.168.1.2
			// 2. 192.168.1
			// 3. 192.168
			// 4. 192
			ip = ip >> 8;

		}

		return sb.toString();
	}
}

