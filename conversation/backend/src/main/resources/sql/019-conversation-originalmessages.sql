CREATE TABLE conversation.originalmessages (
  "id" VARCHAR(36) NOT NULL PRIMARY KEY,
  "body" TEXT
);

CREATE INDEX idx_originalmessages ON conversation.originalmessages (id);

ALTER TABLE conversation.messages ADD "contentVersion" INTEGER DEFAULT 0;