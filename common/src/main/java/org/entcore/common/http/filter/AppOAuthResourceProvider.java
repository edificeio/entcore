/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.common.http.filter;

import fr.wseduc.webutils.DefaultAsyncResult;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import fr.wseduc.webutils.security.SecureHttpServerRequest;
import fr.wseduc.webutils.security.oauth.DefaultOAuthResourceProvider;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import static org.entcore.common.aggregation.MongoConstants.TRACE_TYPE_OAUTH;
import org.entcore.common.cache.CacheService;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.StringUtils;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppOAuthResourceProvider extends DefaultOAuthResourceProvider {
	private static Logger log = LoggerFactory.getLogger(AppOAuthResourceProvider.class);
	private final Pattern prefixPattern;
	private final EventStore eventStore;
	private final Supplier<Optional<CacheService>> cacheServiceSupplier;
	private Optional<CacheService> cacheServiceOpt;
	private static int DEFAULT_TTL_SECONDS = 3600;
	private final Integer ttl;
	private final EventBus eb;

	public AppOAuthResourceProvider(EventBus eb, String prefix, Supplier<Optional<CacheService>> aCacheService, Optional<Integer> aTtl) {
		super(eb);
		final String p = prefix.isEmpty() ? "portal" : prefix.substring(1);
		prefixPattern = Pattern.compile("(^|\\s)" + p + "(\\s|$)");
		eventStore = EventStoreFactory.getFactory().getEventStore(p);
		cacheServiceSupplier = aCacheService;
		ttl = aTtl.isPresent()? aTtl.get() : DEFAULT_TTL_SECONDS;
		this.eb = eb;

	}

	private Optional<CacheService> getCacheService(){
		if(cacheServiceOpt == null)
			cacheServiceOpt=  cacheServiceSupplier.get();
		return cacheServiceOpt;
	}

	private static String getCacheKey(String token){
		return "AppOAuthResourceProvider:token:" + token;
	}

	private static String getCacheLockKey(String token){
		return "AppOAuthResourceProvider:lock:" + token;
	}

	private void prepareCacheSession(final String remoteUser, final SecureHttpServerRequest request, final Handler<Optional<JsonObject>> handler){
		request.setAttribute("remote_user", remoteUser);
		final Optional<String> sessionId = UserUtils.getSessionId(request);
		if(sessionId.isPresent()){
			//dont need to cache
			handler.handle(Optional.empty());
		} else {
			//password flow
			UserUtils.getSession(eb, request , session -> {
				handler.handle(Optional.ofNullable(session));
			});
		}
	}

	private void cacheOAuthInfos(final String token, final SecureHttpServerRequest request, final JsonObject payload, final Handler<AsyncResult<JsonObject>> handler){
		getCacheService().get().get(getCacheLockKey(token), resLock -> {
			if(resLock.succeeded() && resLock.result().isPresent() && "true".equals(resLock.result().get())){
				//already cached (cache only first time to avoid token infinite refresh)
				super.getOAuthInfos(request, payload, handler);
			}else{
				//do cache
				super.getOAuthInfos(request, payload, resOauth -> {
					if(resOauth.succeeded()){
						prepareCacheSession(resOauth.result().getString("remote_user"), request, resSession -> {
							final JsonObject cache = new JsonObject();
							cache.put("oauth", resOauth.result());
							if(resSession.isPresent()){
								cache.put("session", resSession.get());
							}
							getCacheService().get().upsert(getCacheLockKey(token), "true",this.ttl * 2, r -> {});
							getCacheService().get().upsert(getCacheKey(token), cache.encode(), this.ttl, resCache ->{
								handler.handle(resOauth);
							});
						});
					}else{
						handler.handle(resOauth);
					}
				});
			}
		});
	}

	private void loadCache(final String token, final Handler<Optional<JsonObject>> handler){
		getCacheService().get().get(getCacheKey(token), resCache -> {
			if(resCache.succeeded() && resCache.result().isPresent()){
				try{
					handler.handle(Optional.of(new JsonObject(resCache.result().get())));
				}catch(Exception e){
					log.error("[loadCacheSession] Failed to parse cache: ",e);
					handler.handle(Optional.empty());
				}
			}else{
				handler.handle(Optional.empty());
			}
		});
	}

	@Override
	protected void getOAuthInfos(final SecureHttpServerRequest request, final JsonObject payload, final Handler<AsyncResult<JsonObject>> origHandler){
		if(getCacheService().isPresent()){
			request.pause();
			final Handler<AsyncResult<JsonObject>> handler = e -> {
				request.resume();
				origHandler.handle(e);
			};
			Optional<String> token = getTokenId(request);
			if(token.isPresent()){
				request.pause();
				final String tokenStr = token.get();
				loadCache(tokenStr, resCache -> {
					if(resCache.isPresent() && resCache.get().containsKey("oauth")){
						request.resume();
						final JsonObject oauth = resCache.get().getJsonObject("oauth");
						final JsonObject session = resCache.get().getJsonObject("session");
						if(session!=null) {
							//#35187 dont cache the cache attribute (because data could have been changed accross queries)
							session.put("cache", new JsonObject());
							request.setSession(session);
						}
						handler.handle(new DefaultAsyncResult<>(oauth));
					}else{
						cacheOAuthInfos(tokenStr, request, payload, r->{
							request.resume();
							handler.handle(r);
						});
					}
				});
			}else{
				super.getOAuthInfos(request, payload, handler);
			}
		} else {
			super.getOAuthInfos(request, payload, origHandler);
		}
	}

	@Override
	protected boolean customValidation(SecureHttpServerRequest request) {
		final String scope = request.getAttribute("scope");
		// createStatsEvent(request);
		return isNotEmpty(scope) &&
				(prefixPattern.matcher(scope).find() ||
						request.path().contains("/auth/internal/userinfo") ||
						(scope.contains("userinfo") && request.path().contains("/auth/oauth2/userinfo")) ||
						(scope.contains("userinfo") && request.path().contains("/auth/oauth2/token")) ||
						("OAuthSystemUser".equals(request.getAttribute("remote_user")) && isNotEmpty(request.getAttribute("client_id"))) ||
						(scope.contains("myinfos") && request.path().contains("/directory/myinfos")) ||
						(scope.contains("myinfos-ext") && request.path().contains("/directory/myinfos-ext")) ||
						(scope.contains("e-tude") && request.path().contains("/directory/e-tude")) ||
						(scope.contains("saooti") && request.path().contains("/directory/saooti"))

				);
						//(scope.contains("openid") && request.path().contains())
	}

	private void createStatsEvent(SecureHttpServerRequest request) {
		UserInfos user = new UserInfos();
		user.setUserId(request.getAttribute("remote_user"));
		eventStore.createAndStoreEvent(TRACE_TYPE_OAUTH, user, new JsonObject()
				.put("path", request.path()).put("override-module", request.getAttribute("client_id")));
	}


	private static final Pattern REGEXP_AUTHORIZATION = Pattern.compile("^\\s*(?:.+,\\s*)?(OAuth|Bearer)\\s+([^\\s\\,]*)");

	public static Optional<String> getTokenId(HttpServerRequest request)
	{
		Optional<String> token = getTokenHeader(request);

		if(!token.isPresent()){
			token = getTokenParam(request);
		};

		return token;
	}

	public static Optional<String> getTokenHeader(final HttpServerRequest request) {
		//get from header
		final String header = request.getHeader("Authorization");
		if (header != null && Pattern.matches("^\\s*(?:.+,\\s*)?(OAuth|Bearer)(.*)$", header)) {
			final Matcher matcher = REGEXP_AUTHORIZATION.matcher(header);
			if (!matcher.find()) {
				return Optional.empty();
			} else {
				final String token = matcher.group(2);
				return Optional.ofNullable(token);
			}
		} else {
			return Optional.empty();
		}
	}

	private static Optional<String> getTokenParam(final HttpServerRequest request){
		final String oauthToken = request.params().get("oauth_token");
		final String accessToken = request.params().get("access_token");
		if (!StringUtils.isEmpty(accessToken)){
			return Optional.ofNullable(accessToken);
		} else if (!StringUtils.isEmpty(oauthToken)){
			return Optional.ofNullable(oauthToken);
		} else {
			return Optional.empty();
		}
	}

}
