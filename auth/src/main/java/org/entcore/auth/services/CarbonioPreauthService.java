package org.entcore.auth.services;

import fr.wseduc.webutils.Either;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CarbonioPreauthService {
	private static final String HMAC_ALGORITHM = "HmacSHA1";

	private final String carbonioRedirectUrl;
	private final String carbonioDomainKey;

	public CarbonioPreauthService(String carbonioRedirectUrl, String carbonioDomainKey) {
		this.carbonioRedirectUrl = carbonioRedirectUrl;
		this.carbonioDomainKey = carbonioDomainKey;
	}

	public Either<String, String> generatePreauthUrl(String account) {
		long timestamp = System.currentTimeMillis();

		Map<String, String> params = new HashMap<>();
		params.put("account", account);
		params.put("by", "name");
		params.put("timestamp", String.valueOf(timestamp));
		params.put("expires", "0");

		Either<String, String> preauthResult = computePreAuth(params);
		return preauthResult.isRight()
				? new Either.Right<>(buildUrl(params, preauthResult.right().getValue()))
				: new Either.Left<>(preauthResult.left().getValue());
	}

	private Either<String, String> computePreAuth(Map<String, String> params) {
		try {
			String preAuthString = new TreeSet<>(params.keySet()).stream()
					.map(params::get)
					.collect(Collectors.joining("|"));

			String hmac = calculateHmac(preAuthString, carbonioDomainKey);
			return new Either.Right<>(hmac);
		} catch (Exception e) {
			return new Either.Left<>("Failed to compute preauth: " + e.getMessage());
		}
	}

	private String calculateHmac(String data, String key) throws Exception {
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(secretKeySpec);
		byte[] rawHmac = mac.doFinal(data.getBytes());
		return bytesToHex(rawHmac);
	}

	private String bytesToHex(byte[] bytes) {
		return IntStream.range(0, bytes.length)
				.mapToObj(i -> String.format("%02x", bytes[i]))
				.collect(Collectors.joining());
	}

	private String buildUrl(Map<String, String> params, String computedPreAuth) {
		return String.format("%s/service/preauth?account=%s&by=%s&timestamp=%s&expires=%s&preauth=%s",
				carbonioRedirectUrl,
				params.get("account"),
				params.get("by"),
				params.get("timestamp"),
				params.get("expires"),
				computedPreAuth
		);
	}
}
