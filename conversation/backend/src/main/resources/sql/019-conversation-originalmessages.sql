CREATE TABLE conversation.originalmessages (
  "message_id" VARCHAR(36) NOT NULL PRIMARY KEY,
  "body" TEXT
);

CREATE INDEX idx_originalmessages ON conversation.originalmessages (message_id);

ALTER TABLE conversation.messages ADD content_version INTEGER DEFAULT 0;

GRANT SELECT, INSERT, UPDATE, DELETE, TRUNCATE ON conversation.originalmessages TO "apps";