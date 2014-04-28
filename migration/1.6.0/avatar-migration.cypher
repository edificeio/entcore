begin transaction
MATCH (ub:UserBook) WHERE HAS(ub.picture) AND ub.picture <> 'no-avatar.jpg' SET ub.picture = '/workspace/document/' + ub.picture;
commit

