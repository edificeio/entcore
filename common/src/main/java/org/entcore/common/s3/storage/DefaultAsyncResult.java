/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.entcore.common.s3.storage;

import io.vertx.core.AsyncResult;

public class DefaultAsyncResult<T> implements AsyncResult<T> {

	private final T object;
	private final Throwable exception;

	public DefaultAsyncResult(T object) {
		this.object = object;
		this.exception = null;
	}

	public DefaultAsyncResult(Throwable exception) {
		this.object = null;
		this.exception = exception;
	}

	@Override
	public T result() {
		return object;
	}

	@Override
	public Throwable cause() {
		return exception;
	}

	@Override
	public boolean succeeded() {
		return exception == null;
	}

	@Override
	public boolean failed() {
		return exception != null;
	}

}
