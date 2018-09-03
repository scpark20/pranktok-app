package network;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;


public class AESCrypto implements Crypto {
	
	Cipher encCipher;
	Cipher decCipher;
	SecretKey secretKey;
	String secretKeyString;
	
	public AESCrypto()
	{
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128);
			secretKey = keyGenerator.generateKey();
			
			encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			encCipher.init(Cipher.ENCRYPT_MODE, secretKey);
			decCipher.init(Cipher.DECRYPT_MODE, secretKey);
			
			EncodedKeySpec secretKeySpec = new X509EncodedKeySpec(secretKey.getEncoded());
			secretKeyString = Base64.encodeToString(secretKeySpec.getEncoded(), Base64.DEFAULT);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	public AESCrypto(String secretKeyString)
	{
		this.secretKeyString = secretKeyString;
		try {
			encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			byte[] secretKeyBytes = Base64.decode(secretKeyString, Base64.DEFAULT);
			EncodedKeySpec secretKeySpec = new X509EncodedKeySpec(secretKeyBytes);
			secretKey = new SecretKeySpec(secretKeySpec.getEncoded(), 0, secretKeySpec.getEncoded().length, "AES"); 
			
			encCipher.init(Cipher.ENCRYPT_MODE, secretKey);
			decCipher.init(Cipher.DECRYPT_MODE, secretKey);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
	public String getSecretKeyString()
	{
		return this.secretKeyString;
	}
	
	public void encode(ByteBuffer inputBuffer, ByteBuffer outputBUffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		encCipher.doFinal(inputBuffer, outputBUffer);
	}
	
	public int encode(byte[] inputArray, int inputLength, byte[] outputArray) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
		inputBuffer.put(inputArray, 0, inputLength);
		inputBuffer.flip();
		
		ByteBuffer outputBuffer = ByteBuffer.allocate(1024);
		encode(inputBuffer, outputBuffer);
		outputBuffer.flip();
		int n = outputBuffer.limit();
		outputBuffer.get(outputArray, 0, n);
		return n;
	}
	
	public void decode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		decCipher.doFinal(inputBuffer, outputBuffer);
	}
	
	public int decode(byte[] inputArray, int inputLength, byte[] outputArray) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
		inputBuffer.put(inputArray, 0, inputLength);
		inputBuffer.flip();
		
		ByteBuffer outputBuffer = ByteBuffer.allocate(1024);
		decode(inputBuffer, outputBuffer);
		outputBuffer.flip();
		int n = outputBuffer.limit();
		outputBuffer.get(outputArray, 0, n);
		return n;
	}
}
