package com.scpark.prankcallclient;

import java.util.HashMap;

import session.SessionInfo;

public class C {
	public static final String PREF_FILE_KEY = "ewfi3jitj3i2f32t23tsget32jt";
	public static final int SAMPLE_RATE = 44100;
	
	public static final int CURRENT_VERSION = 1;
	
	public static final int SESSION_PACKET_SIZE = 32768;
	public static final int VOICE_DATA_SIZE = 2048;
	
	public static final int FROM_FLAG_SIZE = 1;
	
	public static final int FROM_COUNT = 7;
	public static final byte FROM_UDP_PRIVATE = 0;
	public static final byte FROM_UDP_PUBLIC = 1;
	public static final byte FROM_UDP_SERVER = 2;
	public static final byte FROM_TCP_PRIVATE = 3;
	public static final byte FROM_TCP_PUBLIC = 4;
	public static final byte FROM_TCP_SERVER_P2P = 5;
	public static final byte FROM_TCP_SERVER_MAIN = 6;
	
	public static final int PACKET_TYPE_SIZE = 1;
	public static final byte PACKET_TYPE_IP = 0;
	public static final byte PACKET_TYPE_DATA = 1;
	
	public static final int TIMESTAMP_SIZE = 4;
	public static final int SEQ_SIZE = 4;
	public static final int LENGTH_TAG_SIZE = 4;
	public static final int VOICE_MAX_PACKET_SIZE = SessionInfo.PHONE_NUMBER_LENGTH + PACKET_TYPE_SIZE + SEQ_SIZE + FROM_FLAG_SIZE + TIMESTAMP_SIZE + VOICE_DATA_SIZE + LENGTH_TAG_SIZE;
	public static final int FROM_FLAG_POSITION = SessionInfo.PHONE_NUMBER_LENGTH + PACKET_TYPE_SIZE + SEQ_SIZE;
	public static final int VOICE_MIN_PACKET_SIZE = SessionInfo.PHONE_NUMBER_LENGTH + PACKET_TYPE_SIZE + SEQ_SIZE + FROM_FLAG_SIZE + TIMESTAMP_SIZE + LENGTH_TAG_SIZE;
	public static final int VOICE_BUFFER_QUEUE_SIZE = 500;
	
	public static final int UNIT_DELIMITER = 0x1F;
	public static final int RECORD_DELIMITER = 0x1E;
	public static final int OPERATION_DELIMITER = 0x1D;
	
	public static final String UNIT_DELIMITER_STRING = "\u001F";
	public static final String RECORD_DELIMITER_STRING = "\u001E";
	public static final String OPERATION_DELIMITER_STRING = "\u001D";
	
	public static final int HEART_BEAT_TIME_OUT = 30000; //ms
	public static final int HEART_BEAT_PERIOD = 10000; //ms
	
	public static final int LATENCY_CHECK_TIME = 5000;
	public static final int OUTPUT_MESSAGE_LATENCY = 300;
	
	public static final int USER_LIST = 10;
	
	public static final int TYPE_UDP = 0;
	public static final int TYPE_TCP = 1;
	
	public static final int BIT_RATE = 128000;
	public static final int IP_SEND_AMOUNT = 5;
	
	public static final byte RSA_ENCRYPTED_FLAG = 0;
	public static final byte AES_ENCRYPTED_FLAG = 1;
	public static final byte UNENCRYPTED_FLAG = 2;
	public static final int ENABLE_ONLY_THRESHOLD = 50;
}
