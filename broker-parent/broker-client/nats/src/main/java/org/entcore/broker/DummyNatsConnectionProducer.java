package org.entcore.broker;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.ConsumerContext;
import io.nats.client.Dispatcher;
import io.nats.client.ForceReconnectOptions;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamOptions;
import io.nats.client.KeyValue;
import io.nats.client.KeyValueManagement;
import io.nats.client.KeyValueOptions;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.ObjectStore;
import io.nats.client.ObjectStoreManagement;
import io.nats.client.ObjectStoreOptions;
import io.nats.client.Options;
import io.nats.client.Statistics;
import io.nats.client.StreamContext;
import io.nats.client.Subscription;
import io.nats.client.api.ServerInfo;
import io.nats.client.impl.Headers;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Provides a dummy NATS Connection for build time.
 * This will be replaced by a real Connection at runtime.
 */
@ApplicationScoped
public class DummyNatsConnectionProducer {

    @Produces
    @DefaultBean
    @ApplicationScoped
    // This bean will only be active during build time processing
    @IfBuildProfile("build-time")
    public Connection produceDummyConnection() {
        Log.info("Creating dummy NATS Connection for build time");

        return new Connection() {

            @Override
            public void publish(String subject, byte[] body) {
                throw new UnsupportedOperationException("Unimplemented method 'publish'");
            }

            @Override
            public void publish(String subject, Headers headers, byte[] body) {
                throw new UnsupportedOperationException("Unimplemented method 'publish'");
            }

            @Override
            public void publish(String subject, String replyTo, byte[] body) {
                throw new UnsupportedOperationException("Unimplemented method 'publish'");
            }

            @Override
            public void publish(String subject, String replyTo, Headers headers, byte[] body) {
                throw new UnsupportedOperationException("Unimplemented method 'publish'");
            }

            @Override
            public void publish(Message message) {
                throw new UnsupportedOperationException("Unimplemented method 'publish'");
            }

            @Override
            public CompletableFuture<Message> request(String subject, byte[] body) {
                throw new UnsupportedOperationException("Unimplemented method 'request'");
            }

            @Override
            public CompletableFuture<Message> request(String subject, Headers headers, byte[] body) {
                throw new UnsupportedOperationException("Unimplemented method 'request'");
            }

            @Override
            public CompletableFuture<Message> requestWithTimeout(String subject, byte[] body, Duration timeout) {
                throw new UnsupportedOperationException("Unimplemented method 'requestWithTimeout'");
            }

            @Override
            public CompletableFuture<Message> requestWithTimeout(String subject, Headers headers, byte[] body,
                    Duration timeout) {
                throw new UnsupportedOperationException("Unimplemented method 'requestWithTimeout'");
            }

            @Override
            public CompletableFuture<Message> request(Message message) {
                throw new UnsupportedOperationException("Unimplemented method 'request'");
            }

            @Override
            public CompletableFuture<Message> requestWithTimeout(Message message, Duration timeout) {
                throw new UnsupportedOperationException("Unimplemented method 'requestWithTimeout'");
            }

            @Override
            public Message request(String subject, byte[] body, Duration timeout) throws InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'request'");
            }

            @Override
            public Message request(String subject, Headers headers, byte[] body, Duration timeout)
                    throws InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'request'");
            }

            @Override
            public Message request(Message message, Duration timeout) throws InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'request'");
            }

            @Override
            public Subscription subscribe(String subject) {
                throw new UnsupportedOperationException("Unimplemented method 'subscribe'");
            }

            @Override
            public Subscription subscribe(String subject, String queueName) {
                throw new UnsupportedOperationException("Unimplemented method 'subscribe'");
            }

            @Override
            public Dispatcher createDispatcher(MessageHandler handler) {
                throw new UnsupportedOperationException("Unimplemented method 'createDispatcher'");
            }

            @Override
            public Dispatcher createDispatcher() {
                throw new UnsupportedOperationException("Unimplemented method 'createDispatcher'");
            }

            @Override
            public void closeDispatcher(Dispatcher dispatcher) {
                throw new UnsupportedOperationException("Unimplemented method 'closeDispatcher'");
            }

            @Override
            public void addConnectionListener(ConnectionListener connectionListener) {
                throw new UnsupportedOperationException("Unimplemented method 'addConnectionListener'");
            }

            @Override
            public void removeConnectionListener(ConnectionListener connectionListener) {
                throw new UnsupportedOperationException("Unimplemented method 'removeConnectionListener'");
            }

            @Override
            public void flush(Duration timeout) throws TimeoutException, InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'flush'");
            }

            @Override
            public CompletableFuture<Boolean> drain(Duration timeout) throws TimeoutException, InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'drain'");
            }

            @Override
            public void close() throws InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'close'");
            }

            @Override
            public Status getStatus() {
                throw new UnsupportedOperationException("Unimplemented method 'getStatus'");
            }

            @Override
            public long getMaxPayload() {
                throw new UnsupportedOperationException("Unimplemented method 'getMaxPayload'");
            }

            @Override
            public Collection<String> getServers() {
                throw new UnsupportedOperationException("Unimplemented method 'getServers'");
            }

            @Override
            public Statistics getStatistics() {
                throw new UnsupportedOperationException("Unimplemented method 'getStatistics'");
            }

            @Override
            public Options getOptions() {
                throw new UnsupportedOperationException("Unimplemented method 'getOptions'");
            }

            @Override
            public ServerInfo getServerInfo() {
                throw new UnsupportedOperationException("Unimplemented method 'getServerInfo'");
            }

            @Override
            public String getConnectedUrl() {
                throw new UnsupportedOperationException("Unimplemented method 'getConnectedUrl'");
            }

            @Override
            public InetAddress getClientInetAddress() {
                throw new UnsupportedOperationException("Unimplemented method 'getClientInetAddress'");
            }

            @Override
            public String getLastError() {
                throw new UnsupportedOperationException("Unimplemented method 'getLastError'");
            }

            @Override
            public void clearLastError() {
                throw new UnsupportedOperationException("Unimplemented method 'clearLastError'");
            }

            @Override
            public String createInbox() {
                throw new UnsupportedOperationException("Unimplemented method 'createInbox'");
            }

            @Override
            public void flushBuffer() throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'flushBuffer'");
            }

            @Override
            public void forceReconnect() throws IOException, InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'forceReconnect'");
            }

            @Override
            public void forceReconnect(ForceReconnectOptions options) throws IOException, InterruptedException {
                throw new UnsupportedOperationException("Unimplemented method 'forceReconnect'");
            }

            @Override
            public Duration RTT() throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'RTT'");
            }

            @Override
            public StreamContext getStreamContext(String streamName) throws IOException, JetStreamApiException {
                throw new UnsupportedOperationException("Unimplemented method 'getStreamContext'");
            }

            @Override
            public StreamContext getStreamContext(String streamName, JetStreamOptions options)
                    throws IOException, JetStreamApiException {
                throw new UnsupportedOperationException("Unimplemented method 'getStreamContext'");
            }

            @Override
            public ConsumerContext getConsumerContext(String streamName, String consumerName)
                    throws IOException, JetStreamApiException {
                throw new UnsupportedOperationException("Unimplemented method 'getConsumerContext'");
            }

            @Override
            public ConsumerContext getConsumerContext(String streamName, String consumerName, JetStreamOptions options)
                    throws IOException, JetStreamApiException {
                throw new UnsupportedOperationException("Unimplemented method 'getConsumerContext'");
            }

            @Override
            public JetStream jetStream() throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'jetStream'");
            }

            @Override
            public JetStream jetStream(JetStreamOptions options) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'jetStream'");
            }

            @Override
            public JetStreamManagement jetStreamManagement() throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'jetStreamManagement'");
            }

            @Override
            public JetStreamManagement jetStreamManagement(JetStreamOptions options) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'jetStreamManagement'");
            }

            @Override
            public KeyValue keyValue(String bucketName) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'keyValue'");
            }

            @Override
            public KeyValue keyValue(String bucketName, KeyValueOptions options) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'keyValue'");
            }

            @Override
            public KeyValueManagement keyValueManagement() throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'keyValueManagement'");
            }

            @Override
            public KeyValueManagement keyValueManagement(KeyValueOptions options) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'keyValueManagement'");
            }

            @Override
            public ObjectStore objectStore(String bucketName) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'objectStore'");
            }

            @Override
            public ObjectStore objectStore(String bucketName, ObjectStoreOptions options) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'objectStore'");
            }

            @Override
            public ObjectStoreManagement objectStoreManagement() throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'objectStoreManagement'");
            }

            @Override
            public ObjectStoreManagement objectStoreManagement(ObjectStoreOptions options) throws IOException {
                throw new UnsupportedOperationException("Unimplemented method 'objectStoreManagement'");
            }

        };
    }
}