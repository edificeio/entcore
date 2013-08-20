package edu.one.core.infra.mustache;

import edu.one.core.infra.I18n;
import javax.annotation.Nullable;

public class I18nTemplateFunction extends VertxTemplateFunction {

	private I18n i18n;

	public I18nTemplateFunction(I18n i18n) {
		this.i18n = i18n;
	}

	@Override
	public String apply(@Nullable String key) {
		return i18n.translate(key, request.headers().get("Accept-Language"));
	}

}
