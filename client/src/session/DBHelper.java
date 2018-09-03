package session;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "buddies.db";
	public static final String TABLENAME = "buddies";
	
	public static final String PHONENUMBER = "phoneNumber";
	public static final String NICKNAME = "nickName";
	public static final String EMOJICODE = "emojiCode";
	public static final String COUNTRY = "contry";
	
	private static final String SQL_CREATE_ENTRIES = "create table " + TABLENAME + " (" + 
															PHONENUMBER + " VARCHAR, " +
															NICKNAME + " VARCHAR, " +
															EMOJICODE + " INTEGER, " +
															COUNTRY + " VARCHAR)";
	private static final String SQL_DELETE_ENTRIES = "drop table if exists " + TABLENAME; 
			
	public DBHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(SQL_CREATE_ENTRIES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL(SQL_DELETE_ENTRIES);
		onCreate(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		onUpgrade(db, oldVersion, newVersion);
	}
	
	public int delete(SessionInfo sessionInfo)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		int ret = db.delete(TABLENAME, PHONENUMBER + " = ? ", new String[]{sessionInfo.phoneNumber});
		db.close();
		return ret;
	}

	public boolean write(SessionInfo sessionInfo)
	{
		SessionInfo getSessionInfo = select(sessionInfo.phoneNumber);
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(PHONENUMBER, sessionInfo.phoneNumber);
		values.put(NICKNAME, sessionInfo.nickName);
		values.put(EMOJICODE, sessionInfo.emojiCode);
		values.put(COUNTRY, sessionInfo.country);
		
		boolean ret = true;
		//insert
		if(getSessionInfo==null)
		{
			if(db.insert(TABLENAME, null, values)<0)
				ret = false;
		}
		//update
		else
			if(db.update(TABLENAME, values, PHONENUMBER + " = ? ", new String[]{sessionInfo.phoneNumber})>0)
				ret = true;
		
		db.close();
		return ret;
	}
	
	public SessionInfo select(String phoneNumber)
	{
		String[] projection = {NICKNAME, PHONENUMBER, EMOJICODE, COUNTRY};
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query("buddies", projection, PHONENUMBER + " = ? ", new String[]{phoneNumber}, null, null, null);
		
		if(!c.moveToFirst())
			return null;
		
		SessionInfo sessionInfo = new SessionInfo(c.getString(0), c.getString(1), c.getInt(2), c.getString(3));
		c.close();
		db.close();
		return sessionInfo;
	}
	
	public SessionInfo[] selectAll()
	{
		String[] projection = {NICKNAME, PHONENUMBER, EMOJICODE, COUNTRY};
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query("buddies", projection, null, null, null, null, null);
		
		SessionInfo[] sessionInfos = new SessionInfo[c.getCount()];
		c.moveToFirst();
		
		for(int i=0;i<sessionInfos.length;i++)
		{
			sessionInfos[i] = new SessionInfo(c.getString(0), c.getString(1), c.getInt(2), c.getString(3), true, false);
			c.moveToNext();
		}
		c.close();
		db.close();
		return sessionInfos;
	}
}
