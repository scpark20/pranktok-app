package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

public class Address {
	static public String getAddress()
	{
		URL url;
	    InputStream is = null;
	    BufferedReader br;
	    String line;

	    try {
	        url = new URL("http://m.blog.naver.com/pachelbel");
	        is = url.openStream();  // throws an IOException
	        br = new BufferedReader(new InputStreamReader(is));

	        while ((line = br.readLine()) != null) {
	        	int start = line.indexOf("fwreq");
	        	int end = line.indexOf("qerwf");
	        	//Log.i("scpark", line);
	            if(start>=0)
	            {
	            	
	            	return line.substring(start+5, end);
	        
	            }
	        }
	    } catch (MalformedURLException mue) {
	         //mue.printStackTrace();
	    } catch (IOException ioe) {
	         //ioe.printStackTrace();
	    } finally {
	        try {
	            if (is != null) is.close();
	        } catch (IOException ioe) {
	            // nothing to see here
	        }
	    }
	    return null;
	}
}
