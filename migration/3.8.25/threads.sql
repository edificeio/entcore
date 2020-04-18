BEGIN;
INSERT INTO conversation.threads (
SELECT m.thread_id as id, conversation.last(m.date ORDER BY m.date) as date, conversation.last(subject ORDER BY m.date) as subject,
conversation.last("from" ORDER BY m.date) as "from", conversation.last("to" ORDER BY m.date) as "to", conversation.last(cc ORDER BY m.date) as cc,
conversation.last(cci ORDER BY m.date) as cci, conversation.last("displayNames" ORDER BY m.date) as "displayNames"
FROM conversation.usermessages um
JOIN conversation.messages m on um.message_id = m.id
WHERE m.state = 'SENT'
GROUP BY m.thread_id) ON CONFLICT (id) DO NOTHING;
INSERT INTO conversation.userthreads (
SELECT um.user_id as user_id, m.thread_id as thread_id, SUM(CASE WHEN um.unread THEN 1 ELSE 0 END) as unread
FROM conversation.usermessages um
JOIN conversation.messages m on um.message_id = m.id
WHERE m.state = 'SENT'
GROUP BY user_id, m.thread_id) ON CONFLICT (user_id,thread_id) DO NOTHING;
COMMIT;

