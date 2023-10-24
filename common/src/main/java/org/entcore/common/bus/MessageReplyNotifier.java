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

package org.entcore.common.bus;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.MultiMap;

public class MessageReplyNotifier<T> implements Message
{
	private Message<T> inner;
	private Handler<Void> handler;

	public MessageReplyNotifier(Message<T> inner, Handler<Void> handler)
	{
		this.inner = inner;
		this.handler = handler;
	}

	private void notifyReply()
	{
		if(this.handler != null)
		{
			this.handler.handle(null);
			this.handler = null;
		}
	}

	@Override
	public void fail(int failureCode, String message)
	{
		this.notifyReply();
		this.inner.fail(failureCode, message);
	}

	@Override
	public void reply(Object message)
	{
		this.notifyReply();
		this.inner.reply(message);
	}

	@Override
	public void reply(Object message, DeliveryOptions options)
	{
		this.notifyReply();
		this.inner.reply(message, options);
	}

	@Override
	public Future<Message<Object>> replyAndRequest(@Nullable Object message, DeliveryOptions options) {
		this.notifyReply();
		return this.inner.replyAndRequest(message, options);
	}

	@Override
	public Future<Message<Object>> replyAndRequest(@Nullable Object message) {
		this.notifyReply();
		return this.inner.replyAndRequest(message);
	}

	@Override
	public void replyAndRequest(Object message, DeliveryOptions options, Handler replyHandler)
	{
		this.notifyReply();
		this.inner.replyAndRequest(message, options, replyHandler);
	}

	@Override
	public void replyAndRequest(Object message, Handler replyHandler)
	{
		this.notifyReply();
		this.inner.replyAndRequest(message, replyHandler);
	}

	@Override
	public boolean isSend()
	{
		return this.inner.isSend();
	}

	@Override
	public String replyAddress()
	{
		return this.inner.replyAddress();
	}

	@Override
	public T body()
	{
		return this.inner.body();
	}

	@Override
	public MultiMap headers()
	{
		return this.inner.headers();
	}

	@Override
	public String address()
	{
		return this.inner.address();
	}
}
