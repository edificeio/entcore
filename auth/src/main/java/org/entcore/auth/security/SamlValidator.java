/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.auth.security;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.ZLib;
import org.entcore.auth.services.SamlVectorService;
import org.entcore.auth.services.impl.FrEduVecteurService;
import org.entcore.common.neo4j.Neo4j;
import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.*;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.metadata.*;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.security.MetadataCriteria;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.xml.signature.impl.SignatureBuilder;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.vertx.java.busmods.BusModBase;
import org.w3c.dom.Element;

import java.io.*;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import static fr.wseduc.webutils.Server.getEventBus;
import static fr.wseduc.webutils.Utils.isNotEmpty;

public class SamlValidator extends BusModBase implements Handler<Message<JsonObject>> {

	private final Map<String, SignatureTrustEngine> signatureTrustEngineMap = new HashMap<>();
	private final Map<String, EntityDescriptor> entityDescriptorMap = new HashMap<>();
	private SPSSODescriptor spSSODescriptor;
	private RSAPrivateKey privateKey;
	private String issuer;
	private SamlVectorService samlVectorService;
	private Neo4j neo4j;

	private void debug (String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message);
		}
	}


	@Override
	public void start() {
		final EventBus eb = getEventBus(vertx);
		super.start();

		String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
		if (neo4jConfig != null) {
			neo4j = Neo4j.getInstance();
			neo4j.init(vertx, new JsonObject(neo4jConfig));
		}

		try {
			DefaultBootstrap.bootstrap();
			String path = config.getString("saml-metadata-folder");
			if (path == null || path.trim().isEmpty()) {
				logger.error("Metadata folder not found.");
				return;
			}
			issuer = config.getString("saml-issuer");
			if (issuer == null || issuer.trim().isEmpty()) {
				logger.error("Empty issuer");
				return;
			}

			for (String f : vertx.fileSystem().readDirBlocking(path)) {
				loadSignatureTrustEngine(f);
			}
			loadPrivateKey(config.getString("saml-private-key"));
			vertx.eventBus().localConsumer("saml", this);
		} catch (ConfigurationException | MetadataProviderException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			logger.error("Error loading SamlValidator.", e);
		}
	}

	private void loadPrivateKey(String path) throws NoSuchAlgorithmException, InvalidKeySpecException {
		logger.info("loadPrivateKey : " + path);
		if (path != null && !path.trim().isEmpty() && vertx.fileSystem().existsBlocking(path)) {
			byte[] encodedPrivateKey = vertx.fileSystem().readFileBlocking(path).getBytes();
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
			privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
		}
	}

	@Override
	public void handle(Message<JsonObject> message) {
		final String action = message.body().getString("action", "");
		final String response = message.body().getString("response");
		final String idp = message.body().getString("IDP");
		if (!"generate-slo-request".equals(action) &&
				!"generate-authn-request".equals(action) &&
				!"generate-saml-response".equals(action) &&
				(response == null || response.trim().isEmpty())) {
			sendError(message, "invalid.response");
			return;
		}
		try {
			switch (action) {
				case "generate-authn-request" :
					String sp = message.body().getString("SP");
					String acs = message.body().getString("acs");
					boolean sign = message.body().getBoolean("AuthnRequestsSigned", false);
					if (message.body().getBoolean("SimpleSPEntityID", false)) {
						sendOK(message, generateSimpleSPEntityIDRequest(idp, sp));
					} else {
						sendOK(message, generateAuthnRequest(idp, sp, acs, sign));
					}
					break;
				case "generate-saml-response" :
					String serviceProvider = message.body().getString("SP");
					String userId = message.body().getString("userId");
					String nameid = message.body().getString("nameid");
					String host = message.body().getString("host");
					spSSODescriptor = getSSODescriptor(serviceProvider);
					generateSAMLResponse(serviceProvider, userId, nameid,host, message);
					break;
				case "validate-signature":
					sendOK(message, new JsonObject().put("valid", validateSignature(response)));
					break;
				case "decrypt-assertion":
					sendOK(message, new JsonObject().put("assertion", decryptAssertion(response)));
					break;
				case "validate-signature-decrypt":
					final JsonObject res = new JsonObject();
					if (validateSignature(response)) {
						res.put("valid", true).put("assertion", decryptAssertion(response));
					} else {
						res.put("valid", false).put("assertion", (String) null);
					}
					sendOK(message, res);
					break;
				case "generate-slo-request":
					String sessionIndex = message.body().getString("SessionIndex");
					String nameID = message.body().getString("NameID");
					sendOK(message, new JsonObject().put("slo", generateSloRequest(nameID, sessionIndex, idp)));
					break;
				default:
					sendError(message, "invalid.action");
			}
		} catch (Exception e) {
			sendError(message, e.getMessage(), e);
		}
	}


	/**
	 * Build SAMLResponse and convert it in base64
	 *
	 * @param serviceProvider serviceProvider name qualifier
	 * @param userId neo4j userID
	 * @param nameId ameId value
	 * @param message message
	 *
	 *
	 * @throws SignatureException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws UnsupportedEncodingException
	 * @throws MarshallingException
	 */
	public void generateSAMLResponse(final String serviceProvider, final String userId, final String nameId, final String host, final Message<JsonObject> message)
			throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, MarshallingException {
		logger.info("start generating SAMLResponse");
		logger.info("SP : " + serviceProvider);

		final String entngIdpNameQualifier = config.getString("saml-entng-idp-nq");
		if(entngIdpNameQualifier == null) {
			String error = "entngIdpNameQualifier can not be null. You must specify it in auth configuration (saml-entng-idp-nq properties)";
			logger.error(error);
			JsonObject jsonObject = new JsonObject().put("error", error);
			sendOK(message, jsonObject);
		}
		logger.info("entngIdpNameQualifier : " + entngIdpNameQualifier);

		// -- get spSSODescriptor from serviceProvider id --
		if(spSSODescriptor == null) {
			String error = "error SSODescriptor not found for serviceProvider : " + serviceProvider;
			logger.error(error);
			JsonObject jsonObject = new JsonObject().put("error", error);
			sendOK(message, jsonObject);
		}

		// --- TAG Issuer ---
		final Issuer idpIssuer = createIssuer(entngIdpNameQualifier);

		// --- TAG Status ---
		final Status status = createStatus();

		final AssertionConsumerService assertionConsumerService = spSSODescriptor.getDefaultAssertionConsumerService();
		if(assertionConsumerService == null) {
			String error = "error : AssertionConsumerService not found";
			logger.error(error);
			sendError(message, error);
		}

		// --- TAG AttributeStatement ---
		createVectors(userId,host, new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if(event.isRight()) {
					LinkedHashMap<String, List<String>> attributes = new LinkedHashMap<String, List<String>>();

					JsonArray vectors = event.right().getValue();
					if(vectors == null || vectors.size() == 0) {
						String error = "error bulding vectors for user " + userId;
						logger.error(error);
						sendError(message, error);
					}else {

						for ( int i =0 ; i < vectors.size() ; i++ ) {
							List<String> vectorsValue = new ArrayList<>();
							String vectorType = "";

							JsonObject vectorsJsonObject = (vectors.getJsonObject(i));

							for (Iterator<String> iter = ( vectors.getJsonObject(i)).fieldNames().iterator(); iter.hasNext(); ) {
								vectorType = iter.next();
								if(attributes.containsKey(vectorType)){
									vectorsValue = attributes.get(vectorType);
								}
								vectorsValue.add(((JsonObject) vectorsJsonObject).getString(vectorType));
							}
							attributes.put(vectorType, vectorsValue);
						}
					}

					AttributeStatement attributeStatement = createAttributeStatement(attributes);

					// --- TAG Assertion ---
					Assertion assertion = null;
					try {
						assertion = generateAssertion(entngIdpNameQualifier, serviceProvider,
                                nameId, assertionConsumerService.getLocation(), userId);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						sendError(message, e.getMessage(), e);
					}

					if(assertion == null) {
						String error = "error building assertion";
						logger.error(error);
						sendError(message, error);
					}
					assertion.getAttributeStatements().add(attributeStatement);

					// -- attribute Destination (acs) --
					String destination = assertionConsumerService.getLocation();

					// --- Build response --
					Response response = createResponse(new DateTime(), idpIssuer, status, assertion, destination);

					Signature signature = null;
					try {
						signature = createSignature();
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
						sendError(message, e.getMessage());
					}
					//response.setSignature(signature);
					assertion.setSignature(signature);

					ResponseMarshaller marshaller = new ResponseMarshaller();
					Element element = null;
					try {
						element = marshaller.marshall(response);
					} catch (MarshallingException e) {
						logger.error(e.getMessage(), e);
						sendError(message, e.getMessage(), e);
					}

					if (signature != null) {
						try {
							Signer.signObject(signature);
						} catch (org.opensaml.xml.signature.SignatureException e) {
							logger.error(e.getMessage(), e);
							sendError(message, e.getMessage(), e);
						}
					}


					StringWriter rspWrt = new StringWriter();
					XMLHelper.writeNode(element, rspWrt);

					debug("response : "+ rspWrt.toString());
					JsonObject jsonObject = new JsonObject();

					String base64Response = Base64.getEncoder().encodeToString(rspWrt.toString().getBytes()); //, Base64.DONT_BREAK_LINES);
					debug("base64Response : "+ base64Response);
					jsonObject.put("SAMLResponse64",base64Response);

					jsonObject.put("destination",destination);

					sendOK(message, jsonObject);
				} else {
					String error = "error bulding vectors for user " + userId + " :";
					logger.error(error);
					logger.error(event.left().getValue());
					sendError(message, error);
				}
			}
		});
	}

	/**
	 * Create Success status
	 *
	 * @return the status
	 */
	private Status createStatus() {
		StatusCodeBuilder statusCodeBuilder = new StatusCodeBuilder();
		StatusCode statusCode = statusCodeBuilder.buildObject();
		statusCode.setValue(StatusCode.SUCCESS_URI);

		StatusBuilder statusBuilder = new StatusBuilder();
		Status status = statusBuilder.buildObject();
		status.setStatusCode(statusCode);

		return status;
	}

	/**
	 * Create signature using private key and public cert specified in file conf
	 * @return the signature
	 * @throws Throwable
	 */
	private Signature createSignature() throws Throwable {
		SignatureBuilder builder = new SignatureBuilder();
		Signature signature = builder.buildObject();

		// create public key (cert) portion of credential
		String publicKeyPath = config.getString("saml-public-key");
		FileInputStream inStream = new FileInputStream(publicKeyPath);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cer = (X509Certificate)cf.generateCertificate(inStream);
		inStream.close();


		// create credential and initialize
		BasicX509Credential credential = new BasicX509Credential();
		credential.setEntityCertificate(cer);
		//credential.setPublicKey(cer.getPublicKey());
		credential.setPrivateKey(privateKey);

		signature.setSigningCredential(credential);
		signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
		signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

		return signature;
	}

	/**
	 * Build the java response
	 * @param issueDate date of generation
	 * @param issuer issuer (must be the same as the nameid->namequalifier)
	 * @param status the status
	 * @param assertion the assertion
	 * @param destination the acs location
	 * @return the java Response
	 */
	private Response createResponse(final DateTime issueDate, Issuer issuer, Status status, Assertion assertion, String destination) {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		Response response = responseBuilder.buildObject();
		// ID must not be a number
		response.setID("ENT_"+UUID.randomUUID().toString());
		response.setIssueInstant(issueDate);
		response.setVersion(SAMLVersion.VERSION_20);
		response.setIssuer(issuer);
		response.setStatus(status);
		response.setDestination(destination);
		response.getAssertions().add(assertion);
		return response;
	}


	/**
	 * Build the java assertion
	 *
	 * @param idp identity provider name qualifier
	 * @param serviceProvider service provider name qualifier
	 * @param nameId nameId value
	 * @param recipient recipient of the assertion (SP Assertion Consumer Service)
	 * @param userId user id neo4j
	 * @return the java assertion
	 *
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	private Assertion generateAssertion(String idp, String serviceProvider, String nameId,
										String recipient, String userId)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		debug("start generating assertion");
		debug("IDP : " + idp);
		debug("SP : " + serviceProvider);

		// Init assertion
		AssertionBuilder assertionBuilder = new AssertionBuilder();

		// --- TAG Assertion ---
		final Assertion assertion = assertionBuilder.buildObject();
		// attribut ID
		// ID must not be a number
		assertion.setID("ENT_"+UUID.randomUUID().toString());
		debug("Assertion ID : " + assertion.getID());

		// attribut IssueInstant
		DateTime authenticationTime = new DateTime();
		assertion.setIssueInstant(authenticationTime);
		debug("IssueInstant : " + assertion.getIssueInstant());

		// --- TAG Issuer ---
		Issuer issuer = createIssuer(idp);
		assertion.setIssuer(issuer);

		// --- TAG Subject ---
		Subject subject = createSubject(nameId, 5, idp, serviceProvider, recipient);
		assertion.setSubject(subject);

		// --- TAG Conditions ---
		ConditionsBuilder conditionsBuilder = new ConditionsBuilder();
		Conditions conditions = conditionsBuilder.buildObject();
		conditions.setNotBefore(authenticationTime);
		DateTime notOnOrAfter = new DateTime();
		notOnOrAfter = notOnOrAfter.plusDays(1);
		conditions.setNotOnOrAfter(notOnOrAfter);

		AudienceRestriction audienceRestriction = new AudienceRestrictionBuilder().buildObject();
		Audience issuerAudience = new AudienceBuilder().buildObject();
		issuerAudience.setAudienceURI(serviceProvider);
		audienceRestriction.getAudiences().add(issuerAudience);

		conditions.getAudienceRestrictions().add(audienceRestriction);
		assertion.setConditions(conditions);

		// --- TAG AuthnStatement ---
		AuthnStatement authnStatement = createAuthnStatement(authenticationTime);
		authnStatement.setSessionIndex(assertion.getID());
		assertion.getAuthnStatements().add(authnStatement);

		return  assertion;
	}


	/**
	 * Build vector(s) representating user according to this profile
	 *
	 * @param userId userId neo4j
	 * @param handler handler containing results
	 */
	private void createVectors(String userId,final String host, final Handler<Either<String, JsonArray>> handler) {
		debug("create user Vector(s)");
		HashMap<String, List<String>> attributes = new HashMap<String, List<String>>();
		final JsonArray jsonArrayResult = new JsonArray();
		// browse supported type vector required by the service provider
		for (final AttributeConsumingService attributeConsumingService : spSSODescriptor.getAttributeConsumingServices()) {
			for(RequestedAttribute requestedAttribute: attributeConsumingService.getRequestAttributes()) {
				String vectorName = requestedAttribute.getName();
				if(vectorName.equals("FrEduVecteur")) {
					samlVectorService = new FrEduVecteurService(neo4j);
					samlVectorService.getVectors(userId, new Handler<Either<String, JsonArray>>() {
						@Override
						public void handle(Either<String, JsonArray> stringJsonArrayEither) {
							if(stringJsonArrayEither.isRight()){
								JsonArray jsonArrayResultTemp = ((JsonArray)stringJsonArrayEither.right().getValue());
								for (int i =0 ; i<jsonArrayResultTemp.size();i++){
									jsonArrayResult.add(jsonArrayResultTemp.getValue(i));
								}
								// add FrEduUrlRetour vector
								for(RequestedAttribute requestedAttribute: attributeConsumingService.getRequestAttributes()) {
									String vectorName = requestedAttribute.getName();
									if(vectorName.equals("FrEduUrlRetour")) {
										JsonObject vectorRetour = new JsonObject().put("FrEduUrlRetour",host);
										jsonArrayResult.add(vectorRetour);
									}
								}
								handler.handle(new Either.Right<String, JsonArray>(jsonArrayResult));
							}

						}
					});

				} else if(requestedAttribute.isRequired() && vectorName.equals("FrEduUrlRetour")) {
					String error = "vector "+vectorName+" not implemented yet";
					logger.error(error);
					handler.handle(new Either.Left<String, JsonArray>(error));
				} else if(vectorName.equals("mail")) {
					String error = "vector "+vectorName+" not implemented yet";
					logger.error(error);
					handler.handle(new Either.Left<String, JsonArray>(error));
				} else {
					if(requestedAttribute.isRequired()) {
						String error = "vector " + vectorName + " not supported for user " + userId;
						logger.error(error);
						handler.handle(new Either.Left<String, JsonArray>(error));
					}else{
						logger.debug("vector " + vectorName + " don't have to be supported.");
					}
				}
			}
		}
	}

	/**
	 * Build attribute statement with specified vectors.
	 *
	 * @param attributes attributes containing vectors
	 *
	 * @return the attributeStatement
	 */
	private AttributeStatement createAttributeStatement(HashMap<String, List<String>> attributes) {
		// create authenticationstatement object
		AttributeStatementBuilder attributeStatementBuilder = new AttributeStatementBuilder();
		AttributeStatement attributeStatement = attributeStatementBuilder.buildObject();

		AttributeBuilder attributeBuilder = new AttributeBuilder();
		if (attributes != null) {
			for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
				Attribute attribute = attributeBuilder.buildObject();
				attribute.setName(entry.getKey());
                attribute.setFriendlyName(entry.getKey());
                attribute.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:basic");

				for (String value : entry.getValue()) {
					XSStringBuilder stringBuilder = new XSStringBuilder();
					XSString attributeValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
					attributeValue.setValue(value);
					attribute.getAttributeValues().add(attributeValue);
				}

				attributeStatement.getAttributes().add(attribute);
			}
		}

		return attributeStatement;
	}

	/**
	 * Returns SPSSODescriptor according to service provider name qualifier
	 * @param serviceProvider service provider name qualifier
	 * @return the SPSSODescriptor if exists null otherwise
	 *
	 */
	private SPSSODescriptor getSSODescriptor (String serviceProvider) {
		EntityDescriptor entityDescriptor = entityDescriptorMap.get(serviceProvider);

		if(entityDescriptor == null) {
			return null;
		}

		SPSSODescriptor spSSODescriptor = entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
		return spSSODescriptor;
	}

	private AuthnStatement createAuthnStatement(final DateTime issueDate) {
		debug("createAuthnStatement with issueDate : " + issueDate);
		// create authcontextclassref object
		AuthnContextClassRefBuilder classRefBuilder = new AuthnContextClassRefBuilder();
		AuthnContextClassRef classRef = classRefBuilder.buildObject();
		classRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		// create authcontext object
		AuthnContextBuilder authContextBuilder = new AuthnContextBuilder();
		AuthnContext authnContext = authContextBuilder.buildObject();
		authnContext.setAuthnContextClassRef(classRef);

		// create authenticationstatement object
		AuthnStatementBuilder authStatementBuilder = new AuthnStatementBuilder();
		AuthnStatement authnStatement = authStatementBuilder.buildObject();
		authnStatement.setAuthnInstant(issueDate);
		authnStatement.setAuthnContext(authnContext);

		return authnStatement;
	}

	private Issuer createIssuer(final String issuerName) {
		debug("createIssuer : " + issuerName);
		// create Issuer object
		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer = issuerBuilder.buildObject();
		issuer.setValue(issuerName);
		return issuer;
	}

	private Subject createSubject(String nameIdValue, Integer samlAssertionDays,
								  String idpNameQualifier, String spNameQualifier,
								  String recipient) {
		debug("createSubject for nameid : " + nameIdValue);
		debug("idpNameQualifier : " + idpNameQualifier);
		debug("spNameQualifier : " + spNameQualifier);

		DateTime currentDate = new DateTime();
		if (samlAssertionDays != null)
			currentDate = currentDate.plusDays(samlAssertionDays);

		// create name element
		NameIDBuilder nameIdBuilder = new NameIDBuilder();
		NameID nameId = nameIdBuilder.buildObject();
		nameId.setValue(nameIdValue);
		nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
		nameId.setNameQualifier(idpNameQualifier);
		nameId.setSPNameQualifier(spNameQualifier);

		SubjectConfirmationDataBuilder dataBuilder = new SubjectConfirmationDataBuilder();
		SubjectConfirmationData subjectConfirmationData = dataBuilder.buildObject();
		subjectConfirmationData.setNotOnOrAfter(currentDate);
		subjectConfirmationData.setRecipient(recipient);

		SubjectConfirmationBuilder subjectConfirmationBuilder = new SubjectConfirmationBuilder();
		SubjectConfirmation subjectConfirmation = subjectConfirmationBuilder.buildObject();
		subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");
		subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

		// create subject element
		SubjectBuilder subjectBuilder = new SubjectBuilder();
		Subject subject = subjectBuilder.buildObject();
		subject.setNameID(nameId);
		subject.getSubjectConfirmations().add(subjectConfirmation);

		return subject;
	}

	private JsonObject generateSimpleSPEntityIDRequest(String idp, String sp) {
		return new JsonObject()
				.put("authn-request", getAuthnRequestUri(idp) + "?SPEntityID=" + sp)
				.put("relay-state", SamlUtils.SIMPLE_RS);
	}

	private JsonObject generateAuthnRequest(String idp, String sp, String acs, boolean sign)
			throws NoSuchFieldException, IllegalAccessException, MarshallingException, IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		final String id = "ENT_" + UUID.randomUUID().toString();

		//Create an issuer Object
		Issuer issuer = new IssuerBuilder().buildObject("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer", "samlp" );
		issuer.setValue(sp);

		final NameIDPolicy nameIdPolicy = new NameIDPolicyBuilder().buildObject();
		nameIdPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
		//nameIdPolicy.setSPNameQualifier("");
		nameIdPolicy.setAllowCreate(true);

		//Create AuthnContextClassRef
		AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
		AuthnContextClassRef authnContextClassRef =
				authnContextClassRefBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
						"AuthnContextClassRef", "saml");
		authnContextClassRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		RequestedAuthnContext requestedAuthnContext = new RequestedAuthnContextBuilder().buildObject();
		requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
		requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

		final AuthnRequest authRequest =  new AuthnRequestBuilder()
				.buildObject("urn:oasis:names:tc:SAML:2.0:protocol", "AuthnRequest", "samlp");
		authRequest.setForceAuthn(false);
		authRequest.setIsPassive(false);
		authRequest.setIssueInstant(new DateTime());
		authRequest.setProtocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		authRequest.setAssertionConsumerServiceURL(acs);
		authRequest.setAssertionConsumerServiceIndex(1);
		authRequest.setAttributeConsumingServiceIndex(1);
		authRequest.setIssuer(issuer);
		authRequest.setNameIDPolicy(nameIdPolicy);
		authRequest.setRequestedAuthnContext(requestedAuthnContext);
		authRequest.setID(id);
		authRequest.setVersion(SAMLVersion.VERSION_20);

		final String anr = SamlUtils.marshallAuthnRequest(authRequest)
				.replaceFirst("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", "");
		final String rs = UUID.randomUUID().toString();
		String queryString = "SAMLRequest=" + URLEncoder.encode(ZLib.deflateAndEncode(anr), "UTF-8") +
				"&RelayState=" + rs;
		if (sign) {
			queryString = sign(queryString);
		}

		return new JsonObject()
				.put("id", id)
				.put("relay-state", rs)
				.put("authn-request", getAuthnRequestUri(idp) + "?" + queryString);
	}

	private String sign(String c) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
		final String content = c + "&SigAlg=http%3A%2F%2Fwww.w3.org%2F2000%2F09%2Fxmldsig%23rsa-sha1";
		java.security.Signature sign = java.security.Signature.getInstance("SHA1withRSA");
		sign.initSign(privateKey);
		sign.update(content.getBytes("UTF-8"));
		return content + "&Signature=" +  URLEncoder.encode(Base64.getEncoder().encodeToString(sign.sign()), "UTF-8"); //, Base64.DONT_BREAK_LINES), "UTF-8");
	}

	private String generateSloRequest(String nameID, String sessionIndex, String idp) throws Exception {
		NameID nId = SamlUtils.unmarshallNameId(nameID);
		NameID nameId = SamlUtils.buildSAMLObjectWithDefaultName(NameID.class);
		nameId.setFormat(nId.getFormat());
		nameId.setValue(nId.getValue());
		String spIssuer = this.issuer;
		if (isNotEmpty(nId.getNameQualifier())) {
			nameId.setNameQualifier(nId.getNameQualifier());
		}
		if (isNotEmpty(nId.getSPNameQualifier())) {
			nameId.setSPNameQualifier(nId.getSPNameQualifier());
			spIssuer = nId.getSPNameQualifier();
		}
		LogoutRequest logoutRequest = SamlUtils.buildSAMLObjectWithDefaultName(LogoutRequest.class);

		logoutRequest.setID("ENT_" + UUID.randomUUID().toString());
		String sloUri = getLogoutUri(idp);
		logoutRequest.setDestination(sloUri);
		logoutRequest.setIssueInstant(new DateTime());

		Issuer issuer = SamlUtils.buildSAMLObjectWithDefaultName(Issuer.class);
		issuer.setValue(spIssuer);
		logoutRequest.setIssuer(issuer);

		SessionIndex sessionIndexElement = SamlUtils.buildSAMLObjectWithDefaultName(SessionIndex.class);

		sessionIndexElement.setSessionIndex(sessionIndex);
		logoutRequest.getSessionIndexes().add(sessionIndexElement);

		logoutRequest.setNameID(nameId);

		String lr = SamlUtils.marshallLogoutRequest(logoutRequest)
				.replaceFirst("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n", "");

		String queryString = "SAMLRequest=" + URLEncoder.encode(ZLib.deflateAndEncode(lr), "UTF-8") + "&RelayState=" + config.getString("saml-slo-relayState", "NULL");

		queryString = sign(queryString);

		if (logger.isDebugEnabled()) {
			logger.debug("lr : " + lr);
			logger.debug("querystring : " + queryString);
		}

		return sloUri + "?" + queryString;
	}

	private String getAuthnRequestUri(String idp) {
		String ssoServiceURI = null;
		EntityDescriptor entityDescriptor = entityDescriptorMap.get(idp);
		if (entityDescriptor != null) {
			for (SingleSignOnService ssos : entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)
					.getSingleSignOnServices()) {
				if (ssos.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
					ssoServiceURI = ssos.getLocation();
				}
			}
		}
		return ssoServiceURI;
	}

	private String getLogoutUri(String idp) {
		String sloServiceURI = null;
		EntityDescriptor entityDescriptor = entityDescriptorMap.get(idp);
		if (entityDescriptor != null) {
			for (SingleLogoutService sls : entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS)
					.getSingleLogoutServices()) {
				if (sls.getBinding().equals(SAMLConstants.SAML2_REDIRECT_BINDING_URI)) {
					sloServiceURI = sls.getLocation();
				}
			}
		}
		return sloServiceURI;
	}

	public boolean validateSignature(String assertion) throws Exception {
		final Response response = SamlUtils.unmarshallResponse(assertion);
		final SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
		Signature signature = response.getSignature();

		if (signature == null) {
			if (response.getAssertions() != null && !response.getAssertions().isEmpty()) {
				for (Assertion a : response.getAssertions()) {
					signature = a.getSignature();
				}
			} else if (response.getEncryptedAssertions() != null && !response.getEncryptedAssertions().isEmpty()) {
				Assertion a = decryptAssertion(response);
				if (a != null) {
					signature = a.getSignature();
				}
			} else {
				logger.error("Assertions not founds.");
				throw new ValidationException("Assertions not founds.");
			}
		}
		if (signature == null) {
			logger.error("Signature not found.");
			throw new ValidationException("Signature not found.");
		}
		profileValidator.validate(signature);

		SignatureTrustEngine sigTrustEngine =  getSignatureTrustEngine(response);
		CriteriaSet criteriaSet = new CriteriaSet();
		criteriaSet.add(new EntityIDCriteria(SamlUtils.getIssuer(response)));
		criteriaSet.add(new MetadataCriteria(IDPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS));
		criteriaSet.add(new UsageCriteria(UsageType.SIGNING));

		return sigTrustEngine.validate(signature, criteriaSet);
	}

	private void loadSignatureTrustEngine(String filePath) throws MetadataProviderException {
		logger.info(filePath);
		FilesystemMetadataProvider metadataProvider = new FilesystemMetadataProvider(new File(filePath));
		metadataProvider.setParserPool(new BasicParserPool());
		metadataProvider.initialize();
		MetadataCredentialResolver metadataCredResolver = new MetadataCredentialResolver(metadataProvider);
		KeyInfoCredentialResolver keyInfoCredResolver =
				Configuration.getGlobalSecurityConfiguration().getDefaultKeyInfoCredentialResolver();
		EntityDescriptor entityDescriptor = (EntityDescriptor) metadataProvider.getMetadata();
		String entityID = entityDescriptor.getEntityID();
		entityDescriptorMap.put(entityID, entityDescriptor);
		signatureTrustEngineMap.put(entityID,
				new ExplicitKeySignatureTrustEngine(metadataCredResolver, keyInfoCredResolver));
	}

	private SignatureTrustEngine getSignatureTrustEngine(Response response) {
		// IDP Arena urn:fi:ac-paris:ent:1.0
		// IDP Aten  urn:fi:ac-paris:ts:1.0
		String issuer = SamlUtils.getIssuer(response);
		debug("getSignatureTrustEngine from issuer : " + issuer);
		return signatureTrustEngineMap.get(issuer);
	}

	private String decryptAssertion(String response) throws Exception {
		return SamlUtils.marshallAssertion(decryptAssertion(SamlUtils.unmarshallResponse(response)));
	}

	private Assertion decryptAssertion(Response response) throws Exception {
		EncryptedAssertion encryptedAssertion;
		if (response.getEncryptedAssertions() != null && response.getEncryptedAssertions().size() == 1) {
			encryptedAssertion = response.getEncryptedAssertions().get(0);
		} else {
			throw new ValidationException("Encrypted Assertion not found.");
		}

		BasicX509Credential decryptionCredential = new BasicX509Credential();
		decryptionCredential.setPrivateKey(privateKey);

		Decrypter decrypter = new Decrypter(null, new StaticKeyInfoCredentialResolver(decryptionCredential),
				new InlineEncryptedKeyResolver());
		decrypter.setRootInNewDocument(true);

		Assertion assertion = decrypter.decrypt(encryptedAssertion);

		if (assertion != null && assertion.getSubject() != null && assertion.getSubject().getEncryptedID() != null) {
			SAMLObject s = decrypter.decrypt(assertion.getSubject().getEncryptedID());
			if (s instanceof BaseID) {
				assertion.getSubject().setBaseID((BaseID) s);
			} else if (s instanceof NameID) {
				assertion.getSubject().setNameID((NameID) s);
			}
			assertion.getSubject().setEncryptedID(null);
		}

		if (assertion != null && assertion.getAttributeStatements() != null) {
			for (AttributeStatement statement : assertion.getAttributeStatements()) {
				for (EncryptedAttribute ea : statement.getEncryptedAttributes()) {
					Attribute a = decrypter.decrypt(ea);
					statement.getAttributes().add(a);
				}
				statement.getEncryptedAttributes().clear();
			}
		}
		return assertion;
	}

}
