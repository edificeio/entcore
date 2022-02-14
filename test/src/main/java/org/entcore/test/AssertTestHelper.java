package org.entcore.test;

import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class AssertTestHelper {
    private final Vertx vertx;
    private final TestHelper test;

    AssertTestHelper(TestHelper t, Vertx v) {
        this.vertx = v;
        this.test = t;
    }

    public <B> Handler<Either<String,B>> asyncAssertSuccessEither(final Handler<AsyncResult<B>> handler){
        final Handler<Either<String,B>> either = e ->{
            if(e.isLeft()){
                handler.handle(new DefaultAsyncResult<>(new Exception(e.left().getValue())));
            }else{
                handler.handle(new DefaultAsyncResult<>(e.right().getValue()));
            }
        };
        return either;
    }
}