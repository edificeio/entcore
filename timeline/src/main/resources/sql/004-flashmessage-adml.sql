ALTER TABLE flashmsg.messages
ADD COLUMN "author" VARCHAR(100),
ADD COLUMN "structureId" VARCHAR(36),
ADD COLUMN "lastModifier" VARCHAR(100);

CREATE TABLE flashmsg.messages_substructures (
    "message_id" BIGINT NOT NULL,
    "structure_id" VARCHAR(36) NOT NULL,
    PRIMARY KEY (message_id, structure_id),
    FOREIGN KEY(message_id) REFERENCES flashmsg.messages ON DELETE CASCADE
);