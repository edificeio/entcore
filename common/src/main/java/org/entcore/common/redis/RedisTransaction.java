package org.entcore.common.redis;

import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;
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

    private AsyncResult<Response> getResponse(final AsyncResult<Response> result, final boolean clear){
        if(result.succeeded()){
            final Response response = result.result();
            if(clear){
                this.clear();
            }
            return new DefaultAsyncResult<>(response);
        }else{
            if(clear){
                this.clear();
            }
            return new DefaultAsyncResult<>(result.cause());
        }
    }

    private Future<Void> execCommand(){
        final List<Future<?>> futures = new ArrayList<>();
        for(final PendingCommand cmd : this.commands){
            final Promise<Response> future = cmd.promise;
            futures.add(future.future());
            this.redisAPI.send(cmd.command, cmd.args).onComplete(future);
        }
        return Future.all(futures).mapEmpty();
    }

    public void clear(){
        this.commands.clear();
    }

    public Future<Response> commit() {
        final Promise<Response> promise = Promise.promise();
        this.redisAPI.multi(multiRes -> {
            if(multiRes.succeeded()){
                this.execCommand().onComplete(execCommands -> {
                    if(execCommands.succeeded()){
                        this.redisAPI.exec(execResponses-> promise.handle(getResponse(execResponses, true)));
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
    public RedisAPI discard(Handler<AsyncResult<@Nullable Response>> handler) {
        this.redisAPI.multi(multiRes -> {
            if(multiRes.succeeded()){
                this.execCommand().onComplete(execCommands -> {
                    if(execCommands.succeeded()){
                        this.redisAPI.discard(execResponses->{
                            handler.handle(getResponse(execResponses, true));
                        });
                    }else{
                        handler.handle(new DefaultAsyncResult<>(execCommands.cause()));
                    }
                });
            }else{
                handler.handle(new DefaultAsyncResult<>(multiRes.cause()));
            }
        });
        return this;
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
