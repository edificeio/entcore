UPDATE conversation.usermessages as s SET unread = FALSE FROM ( SELECT user_id, message_id FROM conversation.usermessages um JOIN conversation.messages m
on um.message_id = m.id
WHERE state = 'SENT' AND m.from = user_id AND unread= true AND NOT m.to @> to_jsonb(user_id) AND NOT m.cc @> to_jsonb(user_id)) a
WHERE s.user_id=a.user_id AND s.message_id=a.message_id