#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import unicodedata
from neo4jrestclient.client import GraphDatabase

def sanitize(s):
    return ''.join(c for c in unicodedata.normalize('NFD', s) if unicodedata.category(c) != 'Mn').replace(" ", "").replace("-Personnel", "").replace("-Teacher", "").replace("-Student", "").replace("-Guest", "").replace("-Relative", "").replace("-","").replace("'","").lower()

if len(sys.argv) == 2:
    gdb = GraphDatabase("http://%s:7474/db/data/" % sys.argv[1])
    query = "MATCH (g:Group) WHERE NOT(HAS(g.displayNameSearchField)) RETURN g.id as id, g.name as name"
    res = gdb.query(query, {}, returns=(unicode, unicode))
    if res:
        for row in res:
            query2 = "MATCH (g:Group {id:{id}}) SET g.displayNameSearchField = {dnsf}"
            res2 = gdb.query(query2, {"id":row[0], "dnsf":sanitize(row[1])})
else:
    print("hostname ou ip manquant.")

