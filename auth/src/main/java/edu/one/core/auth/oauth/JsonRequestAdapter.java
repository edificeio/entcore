package edu.one.core.auth.oauth;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;

import jp.eisbahn.oauth2.server.models.Request;

public class JsonRequestAdapter implements Request {

	private final JsonObject request;

	public JsonRequestAdapter(JsonObject request) {
		this.request = request;
	}

	@Override
	public String getHeader(String name) {
		return request.getObject("headers").getString(name);
	}

	@Override
	public String getParameter(String name) {
		return request.getObject("params").getString(name);
	}

	@Override
	public Map<String, String> getParameterMap() {
		Map<String, String> params = new HashMap<>();
		for (String attr: request.getObject("params").getFieldNames()) {
			Object v = request.getObject("params").getValue("attr");
			if (v instanceof String) {
				params.put(attr, (String) v);
			}
		}
		return params;
	}

}
