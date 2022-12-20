package org.entcore.common.redis;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.redis.client.*;

import java.util.ArrayList;
import java.util.List;

public class RedisTransaction implements RedisAPI, IRedisClient {
    private final List<PendingCommand> commands = new ArrayList<>();
    private final RedisAPI redisAPI;

    public RedisTransaction(final RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @Override
    public RedisAPI getClient() {
        return this;
    }

    @Override
    public RedisTransaction transaction() {
        return new RedisTransaction(this.redisAPI);
    }

    @Override
    public void close() {
        this.redisAPI.close();
    }

    private AsyncResult<List<Response>> getResponses(final AsyncResult<Response> result, final boolean clear){
        if(result.succeeded()){
            final Response response = result.result();
            final List<Response> responses = new ArrayList<>();
            for (int i = 0; i < response.size(); i++) {
                responses.add(this.commands.get(i).promise.future().result());
            }
            if(clear){
                this.clear();
            }
            return new DefaultAsyncResult<>(responses);
        }else{
            if(clear){
                this.clear();
            }
            return new DefaultAsyncResult<>(result.cause());
        }
    }

    private Future<Void> execCommand(){
        final List<Future> futures = new ArrayList<>();
        for(final PendingCommand cmd : this.commands){
            final Future<Response> future = cmd.promise.future();
            futures.add(future);
            this.redisAPI.send(cmd.command, cmd.args).handle(future);
        }
        return CompositeFuture.all(futures).mapEmpty();
    }

    public void clear(){
        this.commands.clear();
    }

    public Future<List<Response>> commit() {
        final Promise<List<Response>> promise = Promise.promise();
        this.redisAPI.multi(multiRes -> {
            if(multiRes.succeeded()){
                this.execCommand().onComplete(execCommands -> {
                    if(execCommands.succeeded()){
                        this.redisAPI.exec(execResponses->{
                            promise.handle(getResponses(execResponses, true));
                        });
                    }else{
                        promise.fail(execCommands.cause());
                    }
                });
            }else{
                promise.fail(multiRes.cause());
            }
        });
        return promise.future();
    }

    public Future<List<Response>> discard() {
        final Promise<List<Response>> promise = Promise.promise();
        this.redisAPI.multi(multiRes -> {
            if(multiRes.succeeded()){
                this.execCommand().onComplete(execCommands -> {
                    if(execCommands.succeeded()){
                        this.redisAPI.discard(execResponses->{
                            promise.handle(getResponses(execResponses, true));
                        });
                    }else{
                        promise.fail(execCommands.cause());
                    }
                });
            }else{
                promise.fail(multiRes.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Response> send(Command cmd, String... args) {
        final Promise<Response> promise = Promise.promise();
        commands.add(new PendingCommand(cmd, args, promise));
        return promise.future();
    }

    class PendingCommand{
        final Command command;
        final String[] args;
        final Promise<Response> promise;

        PendingCommand(Command command, String[] args, Promise<Response> promise) {
            this.command = command;
            this.promise = promise;
            this.args = args;
        }
    }
}
