package session;

public class SessionInfo {
	public static final int PHONE_NUMBER_LENGTH = 11;
	public static final int EMOJI_START_CODE = 0x1f601;
	public long prankKey;
	public String phoneNumber = "";
	public String nickName = "";
	public int emoji;
	public String country = "";
	
	public SessionInfo()
	{
		this.prankKey = 0;
		this.phoneNumber = "";
		this.nickName = "";
		this.emoji = EMOJI_START_CODE;
	}

	public SessionInfo(String nickName, String phoneNumber)
	{
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
	}
	
	public SessionInfo(long prankKey, String nickName, String phoneNumber)
	{
		this.prankKey = prankKey;
		this.nickName = nickName;
		this.phoneNumber = phoneNumber;
	}
}
