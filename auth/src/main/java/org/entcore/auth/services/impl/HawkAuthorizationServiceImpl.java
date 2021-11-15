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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;

import io.vertx.core.Handler;

public class HawkAuthorizationServiceImpl extends HawkAuthorizationService
{
    private String clientId;

    public HawkAuthorizationServiceImpl(String clientId, String secret)
    {
        super(secret);
        this.clientId = clientId;
    }

    public void authorize(HttpServerRequest request, Handler<Boolean> handler)
    {
        String authHeader = request.getHeader("Authorization");
        HawkAuthorizationComponents comps = this.extractAuthorizationComponents(authHeader, new JsonArray().add("ext"));

        if(comps == null)
        {
            handler.handle(false);
            return;
        }

        request.bodyHandler(new Handler<Buffer>()
        {
            @Override
            public void handle(Buffer b)
            {
                String contentType = request.getHeader("Content-Type");
                String pathAndQuery = request.path() + (request.query() != null ? "?" + request.query() : "");
                String forwardedScheme = request.getHeader("X-Forwarded-Proto");
                String originalScheme = forwardedScheme != null ? forwardedScheme : request.scheme();
                String port = ("https".equals(originalScheme) ? "443" : "80");
                String body = b.toString("UTF-8");

                try
                {
                    String hash = generateDataHash(contentType != null ? contentType : "application/json", body);
                    String mac = generateMAC(comps.timestamp, comps.nonce, request.method().name(), pathAndQuery, request.host(), port, hash, comps.data("ext"));

                    handler.handle(clientId.equals(comps.id) && mac.equals(comps.mac) && hash.equals(comps.hash));
                }
                catch(UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException e)
                {
                    handler.handle(false);
                }
            }
        });
    }
}
