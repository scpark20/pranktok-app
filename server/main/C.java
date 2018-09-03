package main;

import java.nio.ByteBuffer;
import java.util.HashMap;

import session.SessionInfo;

public class C {
	public static long SERVER_IP;
	public static final int CURRENT_VERSION = 1;
	public static final int SESSION_PORT = 22565;
	public static final int VOICE_TCP_PORT = 21421;
	public static final int VOICE_UDP_PORT = 31242;
	
	public static final int SAMPLE_RATE = 44100;
	
	public static final int USER_LIST_COUNT = 10;
	public static final int SESSION_PACKET_SIZE = 512;
	public static final int VOICE_DATA_SIZE = 1024;
	public static final int FROM_FLAG_SIZE = 1;
	public static final byte FROM_PRIVATE = 0;
	public static final byte FROM_PUBLIC = 1;
	public static final byte FROM_SERVER = 2;
	
	public static final int PACKET_TYPE_SIZE = 1;
	public static final byte PACKET_TYPE_IP = 0;
	public static final byte PACKET_TYPE_DATA = 1;
	
	public static final int TIMESTAMP_SIZE = 4;
	public static final int SEQ_SIZE = 4;
	public static final int LENGTH_TAG_SIZE = 4;
	public static final int VOICE_MAX_PACKET_SIZE = SessionInfo.PHONE_NUMBER_LENGTH + PACKET_TYPE_SIZE + FROM_FLAG_SIZE + TIMESTAMP_SIZE + SEQ_SIZE + VOICE_DATA_SIZE + LENGTH_TAG_SIZE;
	public static final int VOICE_MIN_PACKET_SIZE = SessionInfo.PHONE_NUMBER_LENGTH + FROM_FLAG_SIZE + TIMESTAMP_SIZE + SEQ_SIZE + LENGTH_TAG_SIZE;
	public static final int VOICE_BUFFER_QUEUE_SIZE = 100;
	public static final int SEQ_INDEX = SessionInfo.PHONE_NUMBER_LENGTH + PACKET_TYPE_SIZE; 
	public static final int UNIT_DELIMITER = 0x1F;
	public static final int RECORD_DELIMITER = 0x1E;
	public static final int OPERATION_DELIMITER = 0x1D;
	
	public static final String UNIT_DELIMITER_STRING = "\u001F";
	public static final String RECORD_DELIMITER_STRING = "\u001E";
	public static final String OPERATION_DELIMITER_STRING = "\u001D";
	
	public static final int HEART_BEAT_TIME_OUT = 30000; //ms
	public static final int HEART_BEAT_PERIOD = 10000; //ms
	public static final int SESSION_START_TIME = 5000;
	
	public static final int SESSION_HANDLER_COUNT = 100;
	public static final int VOICE_UDP_PORT_COUNT = 100;
	public static final int VOICE_UDP_PORT_OFFER_COUNT = 5;
	
	public static final int VOICE_TCP_PORT_COUNT = 30;
	public static final int VOICE_TCP_TRANSMISSION_TIME_OUT = 3000;
	
	public static final byte RSA_ENCRYPTED_FLAG = 0;
	public static final byte AES_ENCRYPTED_FLAG = 1;
	public static final byte UNENCRYPTED_FLAG = 2;
	public static final int BUFFER_ALLOC_SIZE = 100000;
	public static final int SESSION_ALLOC_SIZE = 5000;
	public static final int ERROR_COUNT_MAXIMUM = -50;
	public static final long THREAD_ALIVE_TIME = 5000;
	
	public static final String ADMIN_PASSWORD = "fi3j2itj32igniengiawnqgti43nqgi43jgijeri23rt39hgf8ewhagieawng";
}
