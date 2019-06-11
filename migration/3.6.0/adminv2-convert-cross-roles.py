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
    now = datetime.now().strftime("%Yy%mm%dd-%Hh%Mm%Ss")
    logFileName = "adminv2-convert-cross-roles." + str(now) + ".log"
    exportFileName = "adminv2-export-cross-roles." + str(now) + ".txt"
    print("Starting adminv2 cross roles conversion...")
    print("Writing log to file: " + logFileName + "...\n")
    logFile = open(logFileName, "w")
    logFile.write("Adminv2 Cross Roles Conversion Log\n")
    logFile.write("Date: " + str(now) + "\n")
    start = timeit.default_timer()
    
    neo4jHost = sys.argv[1]
    preview = False
    if len(sys.argv) == 3 and sys.argv[2] == "preview":
        preview = True
    
    gdb = GraphDatabase("http://%s:7474/db/data/" % neo4jHost)

    # Export Cross Roles
    print("Exporting Roles to file: " + exportFileName + "...")
    exportFile = open(exportFileName, "w")
    exportFile.write("Export Roles information\n")
    exportFile.write("Date: " + str(now) + "\n")
    queryCrossRoles = ("MATCH (role: Role)-[:AUTHORIZE]->(:Action {type: 'SECURED_ACTION_WORKFLOW'})<-[:PROVIDE]-(app: Application) "
                       "WITH role, count(distinct app) as appNb "
                       "WHERE appNb > 1 "
                       "RETURN role.id, role.name")
    resCrossRoles = gdb.query(queryCrossRoles, {}, returns=(unicode, unicode))
    crossRoleIds = []
    exportFile.write("\n")
    exportFile.write("Cross Roles:\n")
    for resCrossRole in resCrossRoles:
        crossRoleIds.append(resCrossRole[0])
        exportFile.write("{id: " + resCrossRole[0].encode('utf-8') + ", name: " + resCrossRole[1].encode('utf-8') + "}\n")

    # Export Relations: cross role -> actions
    queryCrossRoleActions = ("MATCH (r:Role)-[:AUTHORIZE]->(a:Action) "
                             "WHERE r.id IN {roleIds} "
                             "RETURN r.id, collect(a.name)")
    resCrossRoleActions = gdb.query(queryCrossRoleActions, params={"roleIds": crossRoleIds}, returns=(unicode, [unicode]))
    exportFile.write("\n")
    exportFile.write("Cross Role Actions:\n")
    for resCrossRoleAction in resCrossRoleActions:
        exportFile.write("{roleId: " + resCrossRoleAction[0].encode('utf-8') + ", actionNames: [")
        exportFile.write(", ".join(resCrossRoleAction[1]).encode('utf-8'))
        exportFile.write("]}\n")

    # Export Relations: group -> cross role
    queryCrossRoleGroups = ("MATCH (g:Group)-[:AUTHORIZED]->(r:Role) "
                            "WHERE r.id IN {roleIds} "
                            "RETURN r.id, collect(g.id)")
    resCrossRoleGroups = gdb.query(queryCrossRoleGroups, params={"roleIds": crossRoleIds}, returns=(unicode, [unicode]))
    exportFile.write("\n")
    exportFile.write("Cross Role Groups:\n")
    for resCrossRoleGroups in resCrossRoleGroups:
        exportFile.write("{roleId: " + resCrossRoleGroups[0].encode('utf-8') + ", groupIds: [")
        exportFile.write(", ".join(resCrossRoleGroups[1]).encode('utf-8'))
        exportFile.write("]}\n")
    
    # Export Relations: group -> app role
    queryAppRoles = ("MATCH (role: Role)-[:AUTHORIZE]->(:Action {type: 'SECURED_ACTION_WORKFLOW'})<-[:PROVIDE]-(app: Application) "
                     "WITH role, count(distinct app) as appNb "
                     "WHERE appNb = 1 "
                     "MATCH (role)-[:AUTHORIZE]->(action: Action) "
                     "RETURN role.id, role.name, collect(action.name)")
    resAppRoles = gdb.query(queryAppRoles, {}, returns=(unicode, unicode, [unicode]))
    appRoleIds = []
    appRoles = []
    for resAppRole in resAppRoles:
        appRoleIds.append(resAppRole[0])
        appRoles.append({'id': resAppRole[0], 'name': resAppRole[1], 'actions': resAppRole[2]})
    queryAppRoleGroups = ("MATCH (g:Group)-[:AUTHORIZED]->(r:Role) "
                          "WHERE r.id IN {roleIds} "
                          "RETURN r.id, collect(g.id)")
    resAppRoleGroups = gdb.query(queryAppRoleGroups, params={"roleIds": appRoleIds}, returns=(unicode, [unicode]))
    exportFile.write("\n")
    exportFile.write("Application Role Groups:\n")
    for resAppRoleGroup in resAppRoleGroups:
        exportFile.write("{roleId: " + resAppRoleGroup[0].encode('utf-8') + ", groupIds: [")
        exportFile.write(", ".join(resAppRoleGroup[1]).encode('utf-8'))
        exportFile.write("]}\n")
    print("Export done.\n")

    # Begin cross roles processing
    print("Processing cross roles conversion...")
    logCreateQueries = []
    tx = None
    if not preview:
        tx = gdb.transaction(for_query=True)
    for appRole in appRoles:
        appRoleId = appRole['id']
        appRoleName = appRole['name']
        appRoleActions = appRole['actions']
        for resCrossRoleAction in resCrossRoleActions:
            crossRoleId = resCrossRoleAction[0]
            crossRoleActions = resCrossRoleAction[1]
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
    print(str(len(logCreateQueries)) + " requests to create unique relation between groups and application role.")
    logFile.write("\n")
    logFile.write("Create these relations if not exist:\n")
    for log in logCreateQueries:
        logFile.write(log.encode('utf-8') + "\n")
    # Delete cross roles
    if not preview:
        queryDelete = ("MATCH (r:Role) "
                       "WHERE r.id IN {roleIds}"
                       "DETACH DELETE r")
        tx.append(queryDelete, params={"roleIds": crossRoleIds}, returns=(unicode))
        print("Deleting Cross Roles...")
        tx.commit()
    logFile.write("\n")
    logFile.write("Delete Cross Roles ids:\n[")
    logFile.write(", ".join(crossRoleIds).encode('utf-8'))
    logFile.write("]\n")
    endMessage = "Done in " + str(round(timeit.default_timer()-start, 3)) + " sec.\n"
    logFile.write("\n")
    logFile.write(endMessage)
    logFile.close()
    print(endMessage)
    print("See " + logFileName + " to check results.")
    print("See " + exportFileName + " to check export.")
else:
    print("Bad arguments: neo4j_host preview?")
