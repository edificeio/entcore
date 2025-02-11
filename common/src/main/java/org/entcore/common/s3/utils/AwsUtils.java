package org.entcore.common.s3.utils;

import fr.wseduc.webutils.security.AWS4Signature;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.entcore.common.s3.exception.SignatureException;
import org.entcore.common.s3.storage.StorageObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AwsUtils {

    private static final Logger log = LoggerFactory.getLogger(AwsUtils.class);

    /** Stores the md5 hash of SSE-C keys.*/
    private static final Map<String, String> SSEC_MD5_HASH_CACHE = new HashMap<>();

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
        if (hash != null && "".equals(hash)) hash = null;

        try {
            AWS4Signature.sign(request, region, accessKey, secretKey, hash);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException | UnsupportedEncodingException e) {
            throw new SignatureException(e.getMessage());
        }
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

    public static String getContentType(String p) {
		try {
			Path source = Paths.get(p);
			return Files.probeContentType(source);
		} catch (IOException e) {
			return "";
		}
	}

  private static String getOrComputeSSECKeyMD5Sum(final String ssecKey) {
    return SSEC_MD5_HASH_CACHE.computeIfAbsent(ssecKey, k -> {
      final String ssecKeyHeaderValue;
      if (ssecKey == null || ssecKey.isEmpty()) {
        ssecKeyHeaderValue = "";
      } else {
        String md5Str = "";
        try {
          byte[] hash = MessageDigest.getInstance("MD5").digest(Base64.getDecoder().decode(ssecKey));
          md5Str = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
          log.error("An error occurred while computing the MD5 hash of a SSE-C key", e);
        }
        ssecKeyHeaderValue = md5Str;
      }
      return ssecKeyHeaderValue;
    });
  }

    public static void setSSEC(HttpClientRequest request, String ssec) {
      request.putHeader("x-amz-server-side-encryption-customer-algorithm", "AES256");
      request.putHeader("x-amz-server-side-encryption-customer-key", ssec);
      request.putHeader("x-amz-server-side-encryption-customer-key-MD5", getOrComputeSSECKeyMD5Sum(ssec));
    }

    public static void setSSECCopy(HttpClientRequest request, String ssec) {
        if(ssec == null || ssec.isEmpty()) return;

        AwsUtils.setSSEC(request, ssec);
        
        request.putHeader("x-amz-copy-source-server-side-encryption-customer-algorithm", request.headers().get("x-amz-server-side-encryption-customer-algorithm"));
		request.putHeader("x-amz-copy-source-server-side-encryption-customer-key", request.headers().get("x-amz-server-side-encryption-customer-key"));
		request.putHeader("x-amz-copy-source-server-side-encryption-customer-key-MD5", request.headers().get("x-amz-server-side-encryption-customer-key-MD5"));
    }
}
