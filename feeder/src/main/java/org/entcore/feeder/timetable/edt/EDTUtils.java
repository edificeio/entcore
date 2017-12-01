/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.feeder.timetable.edt;


import fr.wseduc.webutils.data.XML;
import fr.wseduc.webutils.data.ZLib;
import fr.wseduc.webutils.security.Md5;
import fr.wseduc.webutils.security.Sha256;
import org.entcore.feeder.exceptions.ValidationException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import static fr.wseduc.webutils.Utils.isEmpty;


public class EDTUtils {

	private static final Logger log = LoggerFactory.getLogger(EDTUtils.class);
	private PrivateKey privateKey;
	private final String partnerName;

	public EDTUtils(Vertx vertx, String privateRSAKey, String partnerName) {
		this.partnerName = partnerName;
		vertx.fileSystem().readFile(privateRSAKey, new Handler<AsyncResult<Buffer>>() {
			@Override
			public void handle(AsyncResult<Buffer> event) {
				if (event.succeeded()) {
					KeySpec privateKeySpec = new PKCS8EncodedKeySpec(event.result().getBytes());
					try {
						privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
					} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
						log.error("Error loading EDT RSA private key.", e);
					}
				} else {
					log.error("Error loading EDT RSA private key.", event.cause());
				}
			}
		});
	}

	public String decryptExport(String encryptedExport) throws Exception {
		final JAXBContext jaxbContext = JAXBContext.newInstance(EDTEncryptedExport.class);
		final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		final EDTEncryptedExport edtEncryptedExport = (EDTEncryptedExport) unmarshaller.unmarshal(new File(encryptedExport));

		String encryptedKey = null;
		for (EDTEncryptedExport.CLES.PARTENAIRE p : edtEncryptedExport.getCLES().getPARTENAIRE()) {
			if (p.getNOM() != null && p.getNOM().equals(partnerName)) {
				encryptedKey = p.getValue();
				break;
			}
		}
		if (isEmpty(encryptedKey)) {
			throw new ValidationException("invalid.edt.encrypted.key");
		}
		final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		final byte[] decryptedKey = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
		final byte[] key = Arrays.copyOfRange(decryptedKey, 0, 16);
		final byte[] iv = Arrays.copyOfRange(decryptedKey, 16, 32);
		final byte[] sum = Arrays.copyOfRange(decryptedKey, 32, 48);
		if (!Md5.equality(Arrays.copyOfRange(decryptedKey, 0, 32), sum)) {
			throw new ValidationException("invalid.edt.key");
		}

		final Cipher cipher2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher2.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
		final byte[] content = cipher2.doFinal(edtEncryptedExport.getCONTENU().getValue());

		byte[] decompressedContent;
		if (edtEncryptedExport.getCONTENU().isCompresseAvantChiffrement()) {
			decompressedContent = ZLib.decompress(content);
		} else {
			decompressedContent = content;
		}

		final String xmlContent = XML.format(new String(decompressedContent), 0, true);

		if (!Sha256.equality(content, edtEncryptedExport.getVERIFICATION())) {
			throw new ValidationException("invalid.content.hash");
		}
		return xmlContent;
	}

}
