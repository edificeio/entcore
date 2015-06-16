#!/bin/bash

echo "MATCH (u:User) WHERE HAS(u.displayName) return u.id, LOWER(u.displayName);" | neo4j-shell | tail -n +8 | head -n -4 > /tmp/displayNameSearchField.cypher
sed -i 's/^| /MATCH (u:User {id:/' /tmp/displayNameSearchField.cypher
sed -i 's/ | /}) SET u.displayNameSearchField = /' /tmp/displayNameSearchField.cypher
sed -i 's/\s*|\s*$/;/' /tmp/displayNameSearchField.cypher
# sed -i 1i'begin transaction' /tmp/displayNameSearchField.cypher
# echo "commit" >> /tmp/displayNameSearchField.cypher
iconv -f utf8 -t ascii//TRANSLIT -o /tmp/displayNameSearchField.clean.cypher /tmp/displayNameSearchField.cypher
neo4j-shell -file /tmp/displayNameSearchField.clean.cypher

