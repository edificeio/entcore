CREATE SCHEMA conversation;

CREATE TABLE conversation.messages (
	"id" VARCHAR(36) NOT NULL PRIMARY KEY,
	"parent_id" VARCHAR(36),
    "subject" VARCHAR(255),
    "body" TEXT NOT NULL,
    "from" VARCHAR(36) NOT NULL,
    "fromName" VARCHAR(36),
    "to" JSONB NOT NULL,
    "toName" JSONB,
    "cc" JSONB,
    "ccName" JSONB,
    "displayNames" JSONB,
    "state" VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
    "date" BIGINT NOT NULL,
    FOREIGN KEY(parent_id) REFERENCES conversation.messages ON DELETE SET NULL
);

CREATE TABLE conversation.attachments (
	"id" VARCHAR(36) NOT NULL PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "charset" VARCHAR(10) NOT NULL,
    "filename" VARCHAR(255) NOT NULL,
    "contentType" VARCHAR(50) NOT NULL,
    "contentTransferEncoding" VARCHAR(10) NOT NULL,
    "size" BIGINT NOT NULL
);

CREATE TABLE conversation.folders (
	"id" VARCHAR(36) NOT NULL PRIMARY KEY,
	"parent_id" VARCHAR(36),
    "user_id" VARCHAR(36) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "depth" INTEGER NOT NULL DEFAULT 1,
    "trashed" BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY(parent_id) REFERENCES conversation.folders ON DELETE CASCADE
);

CREATE TABLE conversation.usermessages (
	"user_id" VARCHAR(36) NOT NULL,
	"message_id" VARCHAR(36) NOT NULL,
	"folder_id" VARCHAR(36),
    "trashed" BOOLEAN NOT NULL DEFAULT FALSE,
    "unread" BOOLEAN NOT NULL DEFAULT TRUE,
    "total_quota" BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, message_id),
    FOREIGN KEY(message_id) REFERENCES conversation.messages ON DELETE CASCADE,
    FOREIGN KEY(folder_id) REFERENCES conversation.folders ON DELETE CASCADE
);

CREATE TABLE conversation.usermessagesattachments (
	"user_id" VARCHAR(36) NOT NULL,
	"message_id" VARCHAR(36) NOT NULL,
	"attachment_id" VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, message_id, attachment_id),
    FOREIGN KEY(user_id, message_id) REFERENCES conversation.usermessages ON DELETE CASCADE,
    FOREIGN KEY(attachment_id) REFERENCES conversation.attachments ON DELETE CASCADE
);

CREATE TABLE conversation.scripts (
	"filename" VARCHAR(255) NOT NULL PRIMARY KEY,
	"passed" TIMESTAMP NOT NULL DEFAULT NOW()
);
