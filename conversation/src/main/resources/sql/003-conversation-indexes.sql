CREATE INDEX idx_message_to ON conversation.messages USING GIN ("to");
CREATE INDEX idx_message_cc ON conversation.messages USING GIN ("cc");
CREATE INDEX idx_message_dname ON conversation.messages USING GIN ("displayNames");
