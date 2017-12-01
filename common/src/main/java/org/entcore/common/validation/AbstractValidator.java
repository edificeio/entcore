/*
 * Copyright © WebServices pour l'Éducation, 2018
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. */

package org.entcore.common.validation;


import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public abstract class AbstractValidator<S, T> {

	protected AbstractValidator<S, T> next;

	protected abstract void validate(T object, S context, Handler<AsyncResult<Void>> handler);

	public void process(final T object, final S context, final Handler<AsyncResult<Void>> handler) {
		validate(object, context, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.failed() || next == null) {
					handler.handle(event);
				} else {
					next.process(object, context, handler);
				}
			}
		});
	}

	public void setNext(AbstractValidator<S, T> next) {
		this.next = next;
	}

}
