ALTER TABLE flashmsg.messages
    ADD COLUMN "userPositions" JSONB NOT NULL DEFAULT '[]';