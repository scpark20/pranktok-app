package session;

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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.CharSet;

import main.LOG;
import voice.PacketTokenizer;

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
			secretKeyString = Base64.encodeBase64String(secretKeySpec.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public AESCrypto(String secretKeyString)
	{
		this.secretKeyString = secretKeyString;
			try {
				encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
				decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
				byte[] secretKeyBytes = Base64.decodeBase64(secretKeyString);
				EncodedKeySpec secretKeySpec = new X509EncodedKeySpec(secretKeyBytes);
				secretKey = new SecretKeySpec(secretKeySpec.getEncoded(), 0, secretKeySpec.getEncoded().length, "AES"); 
				
				encCipher.init(Cipher.ENCRYPT_MODE, secretKey);
				decCipher.init(Cipher.DECRYPT_MODE, secretKey);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	
		
	public void decode(ByteBuffer inputBuffer, ByteBuffer outputBuffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		decCipher.doFinal(inputBuffer, outputBuffer);
	}
	
	public static void main(String args[])
	{
		ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
		ByteBuffer encodedBuffer = ByteBuffer.allocate(1024);
		
		AESCrypto crypto = new AESCrypto();
		String secretKeyString = crypto.getSecretKeyString();
	
		LOG.I(secretKeyString);
		inputBuffer.putLong(13513523532L);
		inputBuffer.putLong(13513523532L);
		
		inputBuffer.flip();
	
		AESCrypto cryptoClient = new AESCrypto(secretKeyString);
			try {
				cryptoClient.encode(inputBuffer, encodedBuffer);
			} catch (ShortBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		
		encodedBuffer.flip();
		
		ByteBuffer decodedBuffer = ByteBuffer.allocate(1024);
			try {
				crypto.decode(encodedBuffer, decodedBuffer);
			} catch (ShortBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		decodedBuffer.flip();
		long longValue = decodedBuffer.getLong();
		
		LOG.I(longValue+"");
	}
}
