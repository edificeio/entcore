package edu.one.core.infra.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

/**
 *
 * Implemantation of MustaccheFactory that bypass cache to refresh
 * TODO improve 
 */
public class DevMustacheFactory extends DefaultMustacheFactory  {

	public DevMustacheFactory(String resourceRoot) {
		super(resourceRoot);
	}

	@Override
	public Mustache compile(String name) {
		return mc.compile(name);
	}
}
