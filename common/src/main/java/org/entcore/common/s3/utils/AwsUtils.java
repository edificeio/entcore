package org.entcore.common.s3.utils;

import fr.wseduc.webutils.security.AWS4Signature;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.s3.exception.SignatureException;
import org.entcore.common.s3.storage.StorageObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AwsUtils {

    private static final Logger log = LoggerFactory.getLogger(AwsUtils.class);

    public static void sign(HttpClientRequest request, String accessKey, String secretKey, String region) throws SignatureException {
        sign(request, accessKey, secretKey, region, "");
    }

    public static void sign(HttpClientRequest request, String accessKey, String secretKey, String region, StorageObject object) throws SignatureException {
        sign(request, accessKey, secretKey, region, object.getBuffer());
    }

    public static void sign(HttpClientRequest request, String accessKey, String secretKey, String region, Buffer buffer) throws SignatureException {
        sign(request, accessKey, secretKey, region, getDigest(buffer));
    }

    public static void sign(HttpClientRequest request, String accessKey, String secretKey, String region, String hash) throws SignatureException {
        if (hash.equals("")) hash = null;

        try {
            AWS4Signature.sign(request, region, accessKey, secretKey, hash);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | UnsupportedEncodingException e) {
            throw new SignatureException(e.getMessage());
        }
    }

    public static void signBodyString(HttpClientRequest request, String accessKey, String secretKey, String region, String bodyString) throws SignatureException {
        sign(request, accessKey, secretKey, region, getDigest(bodyString.getBytes()));
    }

    public static String getDigest(Buffer buffer) {
        return getDigest(buffer.getBytes());
    }

    public static String getDigest(byte[] bytes) {
        MessageDigest md;
		try {
        	md = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);
			return null;
		}
		md.update(bytes);

        StringBuilder hexHash = new StringBuilder();
        for (byte _byte: md.digest()) hexHash.append(Integer.toString((_byte & 0xff) + 0x100, 16).substring(1));

        return hexHash.toString();
    }

}
