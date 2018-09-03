package session;

public class SessionInfo {
	
	public static final int PHONE_NUMBER_LENGTH = 11;
	public static final String PHONENUMBER_DEFAULT = "02000000000";
	public long prankKey;
	public int emojiCode;
	public String phoneNumber = PHONENUMBER_DEFAULT;
	public String nickName;
	public String country;
	public boolean buddy = false;
	public boolean connect = false;
	
	public SessionInfo()
	{
		this.phoneNumber = PHONENUMBER_DEFAULT;
		this.nickName = "";  
	}

	public SessionInfo(String nickName, String phoneNumber, int emojiCode)
	{
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
		this.emojiCode = emojiCode;
	}
	
	public SessionInfo(String nickName, String phoneNumber, int emojiCode, String country)
	{
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
		this.emojiCode = emojiCode;
		this.country = country;
		this.connect = true;
	}
	
	public SessionInfo(String nickName, String phoneNumber, int emojiCode, String country, boolean buddy, boolean connect)
	{
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
		this.emojiCode = emojiCode;
		this.country = country;
		this.buddy = buddy;
		this.connect = connect;
	}
	
	public SessionInfo(long prankKey, String nickName, String phoneNumber, int emojiCode, String country) {
		// TODO Auto-generated constructor stub
		this.prankKey = prankKey;
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
		this.emojiCode = emojiCode;
		this.country = country;
	}
	
	public SessionInfo(long prankKey, String nickName, String phoneNumber, int emojiCode) {
		// TODO Auto-generated constructor stub
		this.prankKey = prankKey;
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
		this.emojiCode = emojiCode;
	}

	public SessionInfo(long prankKey, String nickName, int emojiCode) {
		// TODO Auto-generated constructor stub
		this.prankKey = prankKey;
		this.nickName = nickName;
		this.phoneNumber = PHONENUMBER_DEFAULT;
		this.emojiCode = emojiCode;
	}

}
