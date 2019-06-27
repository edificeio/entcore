/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
		final byte[] decryptedKey = cipher.doFinal(Base64.getDecoder().decode(encryptedKey.replace("\n","")));
		final byte[] key = Arrays.copyOfRange(decryptedKey, 0, 16);
		final byte[] iv = Arrays.copyOfRange(decryptedKey, 16, 32);
		final byte[] sum = Arrays.copyOfRange(decryptedKey, 32, 48);
		if (!Md5.equality(Arrays.copyOfRange(decryptedKey, 0, 32), sum)) {
			throw new ValidationException("invalid.edt.key");
		}

		final Cipher cipher2 = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher2.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
		final byte[] content = cipher2.doFinal(edtEncryptedExport.getCONTENU().getValue());

		final byte[] decompressedContent;
		if (edtEncryptedExport.getCONTENU().isCompresseAvantChiffrement()) {
			decompressedContent = ZLib.decompress(content);
		} else {
			decompressedContent = content;
		}

		final String decompressedContentString;
		if (decompressedContent.length > 3 && decompressedContent[0] == (byte) 0xEF &&
				decompressedContent[1] == (byte) 0xBB && decompressedContent[2] == (byte) 0xBF) {
			decompressedContentString = new String(Arrays.copyOfRange(decompressedContent, 3, decompressedContent.length));
		} else {
			decompressedContentString = new String(decompressedContent);
		}

		final String xmlContent = XML.format(decompressedContentString, 0, true);

		if (!Sha256.equality(content, edtEncryptedExport.getVERIFICATION())) {
			throw new ValidationException("invalid.content.hash");
		}
		return xmlContent;
	}

}
