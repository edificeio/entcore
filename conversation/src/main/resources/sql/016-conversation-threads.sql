CREATE TABLE conversation.threads (
    "id" VARCHAR(36) NOT NULL PRIMARY KEY,
    "date" BIGINT NOT NULL,
    "subject" VARCHAR(1024),
    "from" VARCHAR(36) NOT NULL,
    "to" JSONB NOT NULL,
    "cc" JSONB,
    "cci" JSONB,
    "displayNames" JSONB
);

CREATE TABLE conversation.userthreads (
    "user_id" VARCHAR(36) NOT NULL,
    "thread_id" VARCHAR(36) NOT NULL,
    "nb_unread" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, thread_id),
    FOREIGN KEY(thread_id) REFERENCES conversation.threads(id) ON DELETE CASCADE
);
