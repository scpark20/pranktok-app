package network;

import java.nio.ByteBuffer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

public interface Crypto {
	abstract public void encode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
	abstract public void decode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;
}
