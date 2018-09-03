package main;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import org.apache.commons.codec.binary.Base64;

public class Crypto {
	Cipher encCipher;
	Cipher decCipher;
	
	EncodedKeySpec publicKeySpec;
	PrivateKey privKey;
	
	public Crypto() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
	{
		encCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		decCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair =generator.generateKeyPair();
		
		PublicKey pubKey = keyPair.getPublic();
		PrivateKey privKey = keyPair.getPrivate();
	
		publicKeySpec = new X509EncodedKeySpec(pubKey.getEncoded());
		//privateKeySpec = new PKCS8EncodedKeySpec(privKey.getEncoded());
		
		decCipher.init(Cipher.DECRYPT_MODE, privKey);
	}
	
	public String getPublicKeyString()
	{
		return Base64.encodeBase64String(publicKeySpec.getEncoded());
	}
	
	public void setPublicKey(String publicKey)
	{
		byte[] publicKeyBytes = Base64.decodeBase64(publicKey);
		this.publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
	}
	
	public void decode(ByteBuffer byteBuffer) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException
	{
		decCipher.doFinal(byteBuffer, byteBuffer);
	}
}
