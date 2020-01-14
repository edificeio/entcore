package org.entcore.common.cache;

public @interface Cache {
    String value() default "";

    CacheScope scope();

    int ttlAsMinutes() default -1;

    boolean usePath() default false;

    boolean useQueryParams() default false;

    CacheOperation operation() default CacheOperation.CACHE;
}
