CREATE SCHEMA flashmsg;

CREATE TABLE flashmsg.messages (
	"id" BIGSERIAL NOT NULL PRIMARY KEY,
	"title" VARCHAR(1024) NOT NULL,
    "contents" JSONB NOT NULL,
    "startDate" TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),
    "endDate" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    "profiles" JSONB NOT NULL,
    "color" VARCHAR(30),
    "customColor" VARCHAR(7),
    "domain" VARCHAR(100),
	"readCount" BIGINT NOT NULL DEFAULT 0,
    "created" TIMESTAMP NOT NULL DEFAULT NOW(),
    "modified" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE flashmsg.messages_read (
    "message_id" BIGINT NOT NULL,
    "user_id" VARCHAR(36) NOT NULL,
    PRIMARY KEY (message_id, user_id),
    FOREIGN KEY(message_id) REFERENCES flashmsg.messages ON DELETE CASCADE
);

CREATE TABLE flashmsg.scripts (
	"filename" VARCHAR(255) NOT NULL PRIMARY KEY,
	"passed" TIMESTAMP NOT NULL DEFAULT NOW()
);
