#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import unicodedata
from neo4jrestclient.client import GraphDatabase

def sanitize(s):
    return ''.join(c for c in unicodedata.normalize('NFD', s) if unicodedata.category(c) != 'Mn').replace(" ", "").replace("-","").replace("'","").lower()

if len(sys.argv) == 2:
    gdb = GraphDatabase("http://%s:7474/db/data/" % sys.argv[1])
    query = "MATCH (u:User) WHERE NOT(HAS(u.firstNameSearchField)) AND NOT(HAS(u.lastNameSearchField)) RETURN u.id as id, u.firstName as firstName, u.lastName as lastName"
    res = gdb.query(query, {}, returns=(unicode, unicode, unicode))
    if res:
        for row in res:
            query2 = "MATCH (u:User {id:{id}}) SET u.firstNameSearchField = {fnsf}, u.lastNameSearchField = {lnsf}"
            res2 = gdb.query(query2, {"id":row[0], "fnsf":sanitize(row[1]), "lnsf":sanitize(row[2])})
else:
    print("hostname ou ip manquant.")

