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

package org.entcore.auth.services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.security.HmacSha256;
import fr.wseduc.webutils.security.Sha256;

import org.entcore.common.utils.StringUtils;

public abstract class HawkAuthorizationService
{
	private static final Pattern AUTHORIZATION_EXTRACT_PATTERN = Pattern.compile("([a-z]+)=\"([^\"]*)\"");
    protected static final String ID_COMPONENT = "id";
    protected static final String TIMESTAMP_COMPONENT = "ts";
    protected static final String NONCE_COMPONENT = "nonce";
    protected static final String MAC_COMPONENT = "mac";
    protected static final String HASH_COMPONENT = "hash";
    protected static final String[] AUTHORIZATION_COMPONENTS = new String[]{ID_COMPONENT, TIMESTAMP_COMPONENT, NONCE_COMPONENT, MAC_COMPONENT, HASH_COMPONENT};

    protected class HawkAuthorizationComponents
    {
        public String id;
        public String timestamp;
        public String nonce;
        public String mac;
        public String hash;
        public JsonObject extraData = new JsonObject();

        public String data(String dataField)
        {
            return this.extraData.getString(dataField);
        }
    }

    private String secret;

    public HawkAuthorizationService(String secret)
    {
        this.secret = secret;
    }

    public abstract void authorize(HttpServerRequest request, Handler<Boolean> handler);

    protected HawkAuthorizationComponents extractAuthorizationComponents(String authHeader, JsonArray extraDataFields)
    {
        if(authHeader == null)
            return null;

        JsonObject comps = new JsonObject();
        final Matcher m = AUTHORIZATION_EXTRACT_PATTERN.matcher(authHeader);

        while(m.find())
            comps.put(m.group(1), m.group(2));

        for(int i = AUTHORIZATION_COMPONENTS.length; i-- > 0;)
            if(StringUtils.isEmpty(comps.getString(AUTHORIZATION_COMPONENTS[i])) == true)
                return null;

        HawkAuthorizationComponents hac = new HawkAuthorizationComponents();

        hac.id = comps.getString(ID_COMPONENT);
        hac.timestamp = comps.getString(TIMESTAMP_COMPONENT);
        hac.nonce = comps.getString(NONCE_COMPONENT);
        hac.mac = comps.getString(MAC_COMPONENT);
        hac.hash = comps.getString(HASH_COMPONENT);

        if(extraDataFields != null)
            for(int i = extraDataFields.size(); i-- > 0;)
                hac.extraData.put(extraDataFields.getString(i), comps.getString(extraDataFields.getString(i)));

        return hac;
    }

    protected String generateMAC(String timestamp, String nonce, String method, String path, String serverName, String port, String hash, String extraData)
    throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException
    {
        String header = "hawk.1.header\n" +
                        timestamp + "\n" +
                        nonce + "\n" +
                        method + "\n" +
                        path + "\n" +
                        serverName + "\n" +
                        port + "\n" +
                        hash + "\n" +
                        (extraData != null ? extraData + "\n" : "");
        return HmacSha256.sign(header, this.secret);
    }

    protected String generateDataHash(String contentType, String body)
    throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException
    {
        if(contentType.contains(";")) // handles content types like application/json; charset=utf-8
            contentType = contentType.substring(0, contentType.indexOf(";"));
        String payload = "hawk.1.payload\n" +
                            contentType + "\n" +
                            body + "\n";
        return Sha256.hashBase64(payload);
    }
}
