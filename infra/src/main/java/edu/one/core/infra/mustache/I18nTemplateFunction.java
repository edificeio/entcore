/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.infra.mustache;

import com.github.mustachejava.TemplateFunction;
import edu.one.core.infra.I18n;
import java.util.Locale;
import javax.annotation.Nullable;
import org.vertx.java.core.http.HttpServerRequest;

/**
 *
 * @author rafik
 */
public class I18nTemplateFunction implements TemplateFunction{

	private I18n i18n;
	private HttpServerRequest request;

	public I18nTemplateFunction(I18n i18n, HttpServerRequest request) {
		this.i18n = i18n;
		this.request = request;
	}

	@Override
	public String apply(@Nullable String key) {
		return i18n.translate(key, request.headers().get("Accept-Language"));
	}

}
