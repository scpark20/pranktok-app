package network;

import java.io.UnsupportedEncodingException;
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
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import android.util.Base64;


public class RSACrypto implements Crypto {
	
	Cipher encCipher;
	Cipher decCipher;
	PublicKey publicKey;
	PrivateKey privateKey;
	String publicKeyString;
	
	public RSACrypto()
	{
		try {
			
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(1024);
			KeyPair keyPair = gen.generateKeyPair();
			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();
			
			encCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			decCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			encCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			decCipher.init(Cipher.DECRYPT_MODE, privateKey);
			
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
			publicKeyString = Base64.encodeToString(publicKeySpec.getEncoded(), Base64.DEFAULT);
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
	
	public RSACrypto(String publicKeyString)
	{
		this.publicKeyString = publicKeyString;
		try {
			encCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			byte[] publicKeyBytes = Base64.decode(publicKeyString, Base64.DEFAULT);
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
			
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			encCipher.init(Cipher.ENCRYPT_MODE, keyFactory.generatePublic(publicKeySpec));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}	
	}
	
	public String getPublicKeyString()
	{
		return this.publicKeyString;
	}
	
	public void encode(ByteBuffer inputBuffer, ByteBuffer outputBUffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		encCipher.doFinal(inputBuffer, outputBUffer);
	}
	
	public String encode(String inputString) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException
	{
		ByteBuffer inputBuffer = ByteBuffer.allocate(4096);
		inputBuffer.put(inputString.getBytes("UTF-8"));
		inputBuffer.flip();
		
		ByteBuffer encodedBuffer = ByteBuffer.allocate(4096);
		encode(inputBuffer, encodedBuffer);
		encodedBuffer.flip();
		byte[] encodedArray = new byte[encodedBuffer.limit()];
		encodedBuffer.get(encodedArray);
		return Base64.encodeToString(encodedArray, Base64.DEFAULT);
	}
	
	public void decode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		decCipher.doFinal(inputBuffer, outputBuffer);
	}
	
	public String decode(String inputString) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException
	{
		byte[] encodedArray = Base64.decode(inputString, Base64.DEFAULT);
		ByteBuffer encodedBuffer = ByteBuffer.allocate(4096);
		encodedBuffer.put(encodedArray);
		encodedBuffer.flip();
		
		ByteBuffer decodedBuffer = ByteBuffer.allocate(4096);
		decode(encodedBuffer, decodedBuffer);
		decodedBuffer.flip();
		byte[] decodedArray = new byte[decodedBuffer.limit()];
		decodedBuffer.get(decodedArray);
		
		return new String(decodedArray, "UTF-8");
	}
	
}
