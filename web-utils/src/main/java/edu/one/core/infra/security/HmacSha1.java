package edu.one.core.infra.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.vertx.java.core.json.impl.Base64;

public class HmacSha1 {

	public static String sign(String content, String key)
			throws NoSuchAlgorithmException, InvalidKeyException,
			IllegalStateException, UnsupportedEncodingException {
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signingKey);
		byte[] signed = mac.doFinal(content.getBytes("UTF-8"));
		return Base64.encodeBytes(signed);
	}

}
