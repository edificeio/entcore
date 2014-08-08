begin transaction

MATCH (m:ConversationMessage), (v:Visible)
WHERE v.id = m.from OR v.id IN m.to OR v.id IN m.cc
SET m.displayNames = coalesce(m.displayNames, []) + (v.id + '$' + coalesce(v.displayName, ' ') + '$' + coalesce(v.name, ' ') + '$' + coalesce(v.groupDisplayName, ' '));

commit
