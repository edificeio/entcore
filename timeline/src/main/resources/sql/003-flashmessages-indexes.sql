CREATE INDEX idx_flashmsg_modified_order ON flashmsg.messages (modified DESC);
CREATE INDEX idx_flashmsg_domain ON flashmsg.messages (domain);
CREATE INDEX idx_flashmsg_startdate ON flashmsg.messages ("startDate");
CREATE INDEX idx_flashmsg_enddate ON flashmsg.messages ("endDate");
