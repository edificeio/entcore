/* Copyright © "Open Digital Education", 2014
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

 *
 */

package org.entcore.directory.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.validation.ValidationException;

public interface UserBookService {
	String PUBLIC = "PUBLIC";
	String PRIVE = "PRIVE";
	List<String> ALLOWED_HOBBIES =
			Arrays.asList("cinema", "sport", "animals", "places", "books", "music");
	List<String> UPDATE_USERBOOK_FIELDS = Arrays.asList("health", "mood", "picture", "motto");
	JsonObject AVATAR_THUMBNAILS = new JsonObject().put("48x48", "").put("100x100", "").put("120x120", "").put("290x290", "").put("381x381", "");

	void update(String userId, JsonObject userBook, Handler<Either<String, JsonObject>> result);

	void get(String userId, Handler<Either<String, JsonObject>> result);

	void getAvatar(String fileId, Optional<String> size, String defaultAVatar, HttpServerRequest request);

	/**
	 * Invalidate the cache of the user's avatar files (Cloudflare Varnish, etc...) but does not remove the files from the
	 * cache.
	 * @param userId Id of the user whise avatar has to be removed from the cache
	 * @return
	 */
	Future<Void> banAvatarCache(String userId);

	Future<Boolean> cleanAvatarCache(List<String> usersId);

	void getCurrentUserInfos(UserInfos user, boolean forceReload, Handler<Either<String, JsonObject>> result);

	void getPersonInfos(String personId, Handler<Either<String, JsonObject>> result);

	void initUserbook(String userId, String theme, JsonObject uacLanguage);

	void setHobbyVisibility(final UserInfos user, final String category, final String visibilityValue, final Handler<Either<String, JsonObject>> handler);

	void setInfosVisibility(final UserInfos user, final String state, final String info, final Handler<Either<String, JsonObject>> handler);

	static String selectHobbies(JsonObject userBookData,String prefix) {
		final List<String> selectClauses = new ArrayList<>();
		final JsonArray listOfHobbies = userBookData.getJsonArray("hobbies", new JsonArray());
		for(int i = 0 ; i < listOfHobbies.size(); i++){
			final  String attr = listOfHobbies.getString(i);
			if (!ALLOWED_HOBBIES.contains(attr)) {
				throw new ValidationException("Invalid hobby name (selectHobbies).");
			}
			selectClauses.add(String.format("%s: COALESCE(%s.hobby_%s,[]) ",attr, prefix,attr));
		}
		return String.format("{%s} as hobbies ", StringUtils.join(selectClauses, ","));
	}

	static JsonArray extractHobbies(JsonObject userBookData, JsonObject result, boolean remove){
		final JsonArray hobbies = new JsonArray();
		final JsonArray listOfHobbies = userBookData.getJsonArray("hobbies", new JsonArray());
		if(result.containsKey("hobbies")){
			final JsonObject fetchedHobbies = result.getJsonObject("hobbies", new JsonObject());
			for(Object h : listOfHobbies){
				final String hobbyName = h.toString();
				final JsonArray currentHobby = fetchedHobbies.getJsonArray(hobbyName, new JsonArray());
				if(currentHobby.size() == 2){
					hobbies.add(new JsonObject().put("visibility", currentHobby.getString(0)).put("category", hobbyName).put("values", currentHobby.getString(1)));
				}else{
					hobbies.add(new JsonObject().put("visibility", "").put("category", hobbyName).put("values", ""));
				}
			}
		}else{
			for(Object hobby : listOfHobbies){
				final String hobbyName = hobby.toString();
				final JsonArray currentHobby = result.getJsonArray("hobby_"+hobbyName, new JsonArray());
				if(currentHobby.size() == 2){
					hobbies.add(new JsonObject().put("visibility", currentHobby.getString(0)).put("category", hobbyName).put("values", currentHobby.getString(1)));
				}else{
					hobbies.add(new JsonObject().put("visibility", "").put("category", hobbyName).put("values", ""));
				}
				if(remove){
					result.remove("hobby_"+hobbyName);
				}
			}
		}
		return hobbies;
	}
}
