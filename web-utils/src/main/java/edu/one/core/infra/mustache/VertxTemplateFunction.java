package edu.one.core.infra.mustache;

import com.github.mustachejava.TemplateFunction;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class VertxTemplateFunction implements TemplateFunction {

	public HttpServerRequest request;
}
