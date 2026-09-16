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

package org.entcore.workspace.service.impl;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import de.maxhenkel.lame4j.Mp3Encoder;
import de.maxhenkel.lame4j.UnknownPlatformException;
import fr.wseduc.webutils.collections.PersistantBuffer;
import fr.wseduc.webutils.data.ZLib;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;


public class AudioRecorderWorker extends BusModBase implements Handler<Message<JsonObject>> {

	// stereo
	private static final int DEFAULT_MP3_CHANNELS = 2;
	// kept from the previous LamejbConfig bitrate
	private static final int DEFAULT_MP3_BITRATE_KBPS = 64;
	// LAME quality scale: 1 (best/slowest) to 9 (worst/fastest)
	private static final int DEFAULT_MP3_QUALITY = 5;

	private Storage storage;
	private WorkspaceHelper workspaceHelper;
	private final Map<String, Integer> sampleRates = new HashMap<>();
	private final Map<String, PersistantBuffer> buffers = new HashMap<>();
	private final Map<String, MessageConsumer<byte[]>> consumers = new HashMap<>();
	private final Set<String> disabledCompression = new HashSet<>();
	private int mp3Channels;
	private int mp3BitrateKbps;
	private int mp3Quality;

	@Override
	public void start() {
		super.start();
		StorageFactory.build(vertx, config)
            .onSuccess(storageFactory -> this.storage = storageFactory.getStorage())
            .onFailure(ex -> logger.error("Error building storage factory", ex));
		workspaceHelper = new WorkspaceHelper(vertx.eventBus(), storage);
		final JsonObject mp3Config = config.getJsonObject("mp3", new JsonObject());
		mp3Channels = mp3Config.getInteger("channels", DEFAULT_MP3_CHANNELS);
		mp3BitrateKbps = mp3Config.getInteger("bitrate-kbps", DEFAULT_MP3_BITRATE_KBPS);
		mp3Quality = mp3Config.getInteger("quality", DEFAULT_MP3_QUALITY);
		vertx.eventBus().localConsumer(AudioRecorderWorker.class.getSimpleName(), this);
	}

	@Override
	public void handle(Message<JsonObject> message) {
		final String action = message.body().getString("action", "");
		final String id = message.body().getString("id");
		switch (action) {
			case "open" :
				final String sampleRate = message.body().getString("sampleRate", "44100");
				sampleRates.put(id, Integer.valueOf(sampleRate));
				open(id, message);
				break;
			case "cancel":
				cancel(id, message);
				break;
			case "save":
				save(id, message);
				break;
			case "rawdata":
				disableCompression(id, message);
				break;
		}
	}

	private void disableCompression(String id, Message<JsonObject> message) {
		disabledCompression.add(id);
		sendOK(message);
	}

	private void save(final String id, final Message<JsonObject> message) {
		final JsonObject session = message.body().getJsonObject("session");
		final String name = message.body().getString("name", "Capture " + System.currentTimeMillis()) + ".mp3";
		final PersistantBuffer buffer = buffers.get(id);
		if (buffer != null) {
			buffer.getBuffer(new Handler<AsyncResult<Buffer>>() {
				@Override
				public void handle(AsyncResult<Buffer> buf) {
					try {
						final Integer sampleRate = sampleRates.getOrDefault(id,44100);
						storage.writeBuffer(id, toMp3(buf.result(), sampleRate), "audio/mp3", name, new Handler<JsonObject>() {
							@Override
							public void handle(JsonObject f) {
								if ("ok".equals(f.getString("status"))) {
									workspaceHelper.addDocument(f,
											UserUtils.sessionToUserInfos(session), name, "mediaLibrary",
											true, new JsonArray(), handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
												@Override
												public void handle(Message<JsonObject> event) {
													if ("ok".equals(event.body().getString("status"))) {
														sendOK(message, event.body());
													} else {
														sendError(message, "workspace.add.error");
													}
												}
											}));
								} else {
									sendError(message, "write.file.error");
								}
								cancel(id, null);
							}
						});
					} catch (Exception e) {
						sendError(message, "encoding.file.error");
						cancel(id, null);
						logger.error("Error writing audio capture.", e);
					}
				}
			});
		} else {
			sendError(message, "missing.buffer.error");
		}
	}

	private void cancel(String id, Message<JsonObject> message) {
		disabledCompression.remove(id);
		sampleRates.remove(id);
		PersistantBuffer buffer = buffers.remove(id);
		if (buffer != null) {
			buffer.clear();
		}
		MessageConsumer<byte[]> consumer = consumers.remove(id);
		if (consumer != null) {
			consumer.unregister();
		}
		if (message != null) {
			sendOK(message);
		}
	}

	private void open(final String id, final Message<JsonObject> message) {
		Handler<Message<byte[]>> handler = new Handler<Message<byte[]>>() {
			@Override
			public void handle(Message<byte[]> chunk) {
				try {
					final PersistantBuffer buf = buffers.get(id);
					final Buffer tmp;
					if (disabledCompression.contains(id)) {
						tmp = Buffer.buffer(chunk.body());
					} else {
						tmp = Buffer.buffer(ZLib.decompress(chunk.body()));
					}
					if (buf != null) {
						buf.appendBuffer(tmp);
					} else {
						PersistantBuffer pb = new PersistantBuffer(vertx, tmp, id);
						pb.exceptionHandler(new Handler<Throwable>() {
							@Override
							public void handle(Throwable event) {
								logger.error("Error with PersistantBuffer " + id, event);
							}
						});
						buffers.put(id, pb);
					}
					chunk.reply(new JsonObject().put("status", "ok"));
				} catch (Exception e) {
					logger.error("Error receiving chunk.", e);
					chunk.reply(new JsonObject().put("status", "error")
							.put("message", "audioworker.chunk.error"));
				}
			}
		};
		MessageConsumer<byte[]> consumer = vertx.eventBus().consumer(AudioRecorderWorker.class.getSimpleName() + id, handler);
		consumers.put(id, consumer);
		sendOK(message);
	}


	/**
	 * Encodes raw 16-bit stereo PCM into MP3 via lame4j.
	 *
	 * @param pcm raw PCM buffer (no WAV header), little-endian 16-bit stereo samples as sent by the client
	 * @param sampleRate PCM sample rate of the capture
	 */
	private Buffer toMp3(Buffer pcm, Integer sampleRate) throws IOException, UnknownPlatformException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// PCM bytes must be reinterpreted as shorts (Mp3Encoder.write() takes short[], not bytes), same little-endian order as sent by the client
		final ShortBuffer samples = ByteBuffer.wrap(pcm.getBytes())
				.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		final short[] pcmSamples = new short[samples.remaining()];
		samples.get(pcmSamples);
		try (Mp3Encoder encoder = new Mp3Encoder(mp3Channels, sampleRate, mp3BitrateKbps, mp3Quality, baos)) {
			encoder.write(pcmSamples);
		}
		return Buffer.buffer(baos.toByteArray());
	}

}
