#!/usr/bin/env python
# -*- coding: utf-8 -*-
# usage:
# ./adminv2-convert-cross-roles.py myNeo4jHost preview
#

import sys
import os
import unicodedata
from datetime import datetime
import timeit
from neo4jrestclient.client import GraphDatabase

if len(sys.argv) >= 2:
    print("Starting adminv2 cross roles conversion...")
    print("Writing log to file: ./adminv2-convert-cross-roles.log...")
    logFile = open("adminv2-convert-cross-roles.log", "w")
    logFile.write("Adminv2 Cross Roles Conversion Log\n")
    logFile.write("Date: " + str(datetime.now()) + "\n")
    start = timeit.default_timer()
    
    neo4jHost = sys.argv[1]
    preview = False
    if len(sys.argv) == 3 and sys.argv[2] == "preview":
        preview = True
    
    gdb = GraphDatabase("http://%s:7474/db/data/" % neo4jHost)
    queryAppRoles = ("MATCH (role: Role)-[:AUTHORIZE]->(:Action {type: 'SECURED_ACTION_WORKFLOW'})<-[:PROVIDE]-(app: Application) "
                     "WITH role, count(distinct app) as appNb "
                     "WHERE appNb = 1 "
                     "MATCH (role)-[:AUTHORIZE]->(action: Action) "
                     "RETURN role.id, role.name, collect(action.name)")
    resAppRoles = gdb.query(queryAppRoles, {}, returns=(unicode, unicode, [unicode]))
    queryCrossRoles = ("MATCH (role: Role)-[:AUTHORIZE]->(:Action {type: 'SECURED_ACTION_WORKFLOW'})<-[:PROVIDE]-(app: Application) "
                       "WITH role, count(distinct app) as appNb "
                       "WHERE appNb > 1 "
                       "MATCH (role)-[:AUTHORIZE]->(action: Action) "
                       "RETURN role.id, collect(action.name)")
    resCrossRoles = gdb.query(queryCrossRoles, {}, returns=(unicode, [unicode]))
    
    logCreateQueries = []
    tx = gdb.transaction(for_query=True)
    for appRole in resAppRoles:
        appRoleId = appRole[0]
        appRoleName = appRole[1]
        appRoleActions = appRole[2]
        for crossRole in resCrossRoles:
            crossRoleId = crossRole[0]
            crossRoleActions = crossRole[1]
            if set(appRoleActions).issubset(set(crossRoleActions)):
                queryGroups = ("MATCH (g:Group)-[:AUTHORIZED]->(r:Role {id: {roleId}}) "
                               "RETURN g.id, g.name")
                resGroups = gdb.query(queryGroups, params={"roleId": crossRoleId}, returns=(unicode, unicode))
                for group in resGroups:
                    groupId = group[0]
                    groupName = group[1]
                    queryCreateGroupAuthorizedAppRole = ("MATCH (g: Group {id: {groupId}}), (r: Role {id: {roleId}}) " 
                                                         "CREATE UNIQUE (g)-[:AUTHORIZED]->(r)")
                    if not preview:
                        tx.append(queryCreateGroupAuthorizedAppRole, params={"groupId": groupId, "roleId": appRoleId})
                    logCreateQueries.append("(" + groupName + ")-[:AUTHORIZED]->(" + appRoleName + ")")
    if not preview:
        tx.commit()
        print("Commit " + str(len(logCreateQueries)) + " requests.")
    logFile.write("\n")
    logFile.write("Create these relations if not exist:\n")
    for log in logCreateQueries:
        logFile.write(log.encode('utf-8') + "\n")
    logFile.close()
    print("Done in " + str(round(timeit.default_timer()-start, 3)) + " sec.")
    print("See ./adminv2-convert-cross-roles.log to check queries.")
else:
    print("Bad arguments: neo4j_host preview?")
