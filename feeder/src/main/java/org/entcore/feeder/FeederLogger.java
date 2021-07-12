package org.entcore.feeder;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.AccessLogger;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.Function;

public class FeederLogger {
    protected static boolean enable = true;
    protected static final Logger log = LoggerFactory.getLogger(FeederLogger.class);
    protected final Function<Void, String> prefix;
    protected final Function<Void, String> suffix;
    public FeederLogger(final Function<Void, String> prefix) {
        this.prefix = prefix;
        this.suffix = e->"";
    }
    public FeederLogger(final Function<Void, String> prefix, final Function<Void, String> suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public static void init(final JsonObject config) {
        FeederLogger.enable = config.getBoolean("log-details", false);
    }

    public void info(final Function<Void, String> message){
        info(message, false);
    }

    public void info(final Function<Void, String> message, final boolean force){
        try{
            if(enable || force) {
                final String temp0 = this.prefix.apply(null);
                final String temp = message.apply(null);
                final String temp2 = this.suffix.apply(null);
                log.info(temp0 + " | "+temp + " | "+temp2);
            }
        }catch(final Exception e){
            log.error("Fail to trace import: ", e);
        }
    }

    public void error(final Function<Void, String> message){
        error(message, null);
    }

    public void error(final Function<Void, String> message, final Throwable exception){
        try{
            final String temp0 = this.prefix.apply(null);
            final String temp = message.apply(null);
            final String temp2 = this.suffix.apply(null);
            log.error(temp0 + " | "+temp + " | "+temp2, exception);
        }catch(final Exception e){
            log.error("Fail to trace import: ", e);
        }
    }

    public void warn(final Function<Void, String> message){
        warn(message, null);
    }

    public void warn(final Function<Void, String> message, final Throwable exception){
        try{
            final String temp0 = this.prefix.apply(null);
            final String temp = message.apply(null);
            final String temp2 = this.suffix.apply(null);
            log.warn(temp0 + " | "+temp + " | "+temp2, exception);
        }catch(final Exception e){
            log.warn("Fail to trace import: ", e);
        }
    }
}
