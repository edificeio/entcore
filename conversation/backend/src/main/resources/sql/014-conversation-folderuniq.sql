-- Skip uniq constraint for existing data NULL are ignored
ALTER TABLE conversation.folders ADD skip_uniq BOOLEAN DEFAULT FALSE;
UPDATE conversation.folders SET skip_uniq = NULL;
CREATE UNIQUE INDEX conversation_foldername_parent_uniq_idx ON conversation.folders (name, user_id, parent_id,skip_uniq) WHERE parent_id IS NOT NULL;
CREATE UNIQUE INDEX conversation_foldername_uniq_idx ON conversation.folders (name, user_id,skip_uniq) WHERE parent_id IS NULL;