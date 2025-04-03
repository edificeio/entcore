ALTER TABLE conversation.messages ADD thread_id  VARCHAR(36);

UPDATE conversation.messages
 SET thread_id = id
 WHERE parent_id IS NULL;

WITH RECURSIVE messagesThred AS (
(
  SELECT m.id, m.parent_id, m.thread_id, m.thread_id as thread_tmp FROM conversation.messages as m WHERE thread_id IS NOT NULL
)
UNION ALL
(
  SELECT m.id, m.parent_id, m.thread_id, messagesThred.thread_tmp as thread_tmp FROM messagesThred, conversation.messages as m
  WHERE m.thread_id IS NULL and m.parent_id = messagesThred.id)
)

UPDATE conversation.messages as m
SET thread_id = messagesThred.thread_tmp
FROM messagesThred
WHERE m.thread_id IS NULL AND m.id = messagesThred.id;

CREATE INDEX idx_thread_id ON conversation.messages ("thread_id");