ALTER TABLE flashmsg.messages
ADD COLUMN "signature" VARCHAR(1024),
ADD COLUMN "signatureColor" VARCHAR(7);