#!/bin/env python

import sys
import traceback
import psycopg2
from datetime import datetime
from psycopg2.extras import Json
from psycopg2 import errorcodes
from neo4jrestclient.client import GraphDatabase

gdb = GraphDatabase("http://localhost:7474/db/data/")
sql = psycopg2.connect(database="ong", host="10.0.81.20", user="web-education", password="We_1234")
sql.autocommit = False
cur = sql.cursor()

limit = 500

# NEO4J QUERIES #
#################

foldersQuery = """
MATCH (uf:ConversationUserFolder), path=(c:Conversation)-[:HAS_CONVERSATION_FOLDER]->(:ConversationUserFolder)-[:HAS_CHILD_FOLDER*0..5]->(uf)
OPTIONAL MATCH (uf)<-[:HAS_CHILD_FOLDER]-(pf)
OPTIONAL MATCH (c)-[trashRel:TRASHED_CONVERSATION_FOLDER]->(uf)
RETURN DISTINCT uf, pf.id as parentId, c.userId as userId, length(path) as depth, count(trashRel) > 0  as trashed
ORDER BY depth;
"""

attachmentsQuery = """
MATCH (ma:MessageAttachment) RETURN ma
SKIP {skip} LIMIT {limit};
"""

msgQuery = """
MATCH (msg:ConversationMessage)
WITH DISTINCT msg
ORDER BY msg.id
SKIP {skip} LIMIT {limit}
MATCH (msg)<-[mLink:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)
OPTIONAL MATCH (msg)-[iLink:INSIDE]->(uf:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0..4]-()<-[:HAS_CONVERSATION_FOLDER]-(c)
OPTIONAL MATCH (uma:MessageAttachment)<-[:HAS_ATTACHMENT]-(msg)<-[mLink]-(f)
WHERE uma.id IN mLink.attachments
WITH DISTINCT msg, c, uf, collect(f.name) as folderNames, count(mLink.unread) as unreadFlag,
collect(distinct uma.id) as attachments,
iLink, sum(distinct uma.size) as totalSize
WITH msg, collect(DISTINCT {
    userId: c.userId,
    folderId: uf.id,
    attachmentsIds: attachments,
    totalSize: totalSize,
    unread: coalesce(unreadFlag = 0, true),
    trashed: coalesce(iLink.trashed OR "TRASH" IN folderNames, false)
}) as userProps
RETURN msg, userProps;
"""

msgParentQuery = """
MATCH (msg:ConversationMessage)-[:PARENT_CONVERSATION_MESSAGE]->(pMsg:ConversationMessage)
WHERE (pMsg)<-[:HAS_CONVERSATION_MESSAGE]-()
RETURN msg.id, pMsg.id as parent_id
SKIP {skip} LIMIT {limit};
"""

# POSTRGESQL QUERIES #
######################

insertFolderQuery = """
    INSERT INTO conversation.folders
        (id, parent_id, user_id, name, depth, trashed)
    VALUES """
insertAttachmentQuery = """
    INSERT INTO conversation.attachments
        (id, name, charset, filename, "contentType", "contentTransferEncoding", size)
    VALUES """
insertMessageQuery = """
    INSERT INTO conversation.messages
        ("id", "subject", "body", "from", "to", "cc", "displayNames", "state", "date")
    VALUES """

updateParentMessageQuery = {
    'prepare': """
        PREPARE updateParentMessage AS
        UPDATE conversation.messages
        SET parent_id = $1
        WHERE id = $2 AND parent_id IS NULL
    """,
    'execute': "EXECUTE updateParentMessage (%s, %s)"
}
insertUserMessageQuery = """
    INSERT INTO conversation.usermessages
        (user_id, message_id, folder_id, trashed, unread, total_quota)
    VALUES """
insertUserMessagesAttachmentQuery = """
    INSERT INTO conversation.usermessagesattachments
        (user_id, message_id, attachment_id)
    VALUES """

# COMPARISON QUERIES #
######################

msgCheck = {
    'text': "Message count",
    'neo': "MATCH (m:ConversationMessage) RETURN count(DISTINCT m)",
    'sql': "SELECT count(*) FROM conversation.messages"
}

attCheck = {
    'text': "Attachments count",
    'neo': """
    MATCH (a:MessageAttachment)
    RETURN count(distinct a)
    """,
    'sql': "SELECT count(*) FROM conversation.attachments"
}

foldersCheck = {
    'text': "Folders count",
    'neo': "MATCH (f:ConversationUserFolder) RETURN count(DISTINCT f)",
    'sql': "SELECT count(*) FROM conversation.folders"
}

userFoldersCheck = {
    'text': "Links: users-->folders",
    'neo': "MATCH (f:ConversationUserFolder)<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation) RETURN count(DISTINCT c)",
    'sql': "SELECT count(distinct user_id) FROM conversation.folders"
}

usersMsgCheck = {
    'text': "Links : users-->messages",
    'neo': """
    MATCH (:ConversationMessage)<-[:HAS_CONVERSATION_MESSAGE]-(:ConversationSystemFolder)<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)
    RETURN count(distinct c)
    """,
    'sql': "SELECT count(distinct user_id) FROM conversation.usermessages"
}

usersMsgAttCheck = {
    'text': "Links : messages-->attachments (1) & users-->attachments (2)",
    'neo': """
    MATCH (a:MessageAttachment)<-[:HAS_ATTACHMENT]-(m:ConversationMessage)<-[mLink:HAS_CONVERSATION_MESSAGE]-(:ConversationSystemFolder)<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)
    WHERE a.id IN mLink.attachments
    RETURN count(distinct c), count(distinct m)
    """,
    'sql': "SELECT count(distinct user_id), count(distinct message_id) FROM conversation.usermessagesattachments"
}


# HELPERS #
###########

def convert(input):
    if isinstance(input, dict):
        return dict((convert(key), convert(value)) for key, value in input.iteritems())
    elif isinstance(input, list):
        return [convert(element) for element in input]
    elif isinstance(input, unicode):
        return input.encode('utf-8')
    else:
        return input


def containsOr(row, item, default, action=(lambda x: x)):
    if item in row:
        return action(row[item])
    return default


def containsOrNone(row, item, action=(lambda x: x)):
    return containsOr(row, item, None, action)


def printRow(row):
    print(row)


def commit(buf):
    sql.commit()


# PAGINATION LOOP #
###################

def pageLoop(query, params, rowAction, loopAction=(lambda: ()), endAction=(lambda: ()), returnList=(), page=0):
    stop = False
    while not stop:
        buf = {}
        print('(' + str(datetime.now()) + ') [loop] page ' + str(page) + ' / ' + str(page * limit) + ' -> ' + str((page + 1) * limit - 1))
        sys.stdout.flush()
        paramsCopy = params.copy()
        paramsCopy.update({'skip': page * limit, 'limit': limit})
        results = gdb.query(query, paramsCopy, returns=returnList)
        #print('(' + str(datetime.now()) + ') [loop][neo] reply : ' + str(len(results)))
        if len(results) > 0:
            for row in results:
                try:
                    rowAction(row, buf)
                except Exception as e:
                    print("Error in row action : %s" % str(e))
            loopAction(buf)
            page = page + 1
        else:
            stop = True
        #print('(' + str(datetime.now()) + ') [loop] end')
    endAction()


# QUERY ACTIONS #
#################


def bufferFolder(row, buf):
    #(id, parent_id, user_id, name, depth, trashed)
    params = (row[0]['id'], row[1], row[2], row[0]['name'][:255].decode('utf-8', 'replace'), row[3], row[4])
    if 'params' not in buf.keys():
        buf['params'] = []
    buf['params'].append(params)


def insertFolder(buf):
    cur.execute(insertFolderQuery + (','.join(cur.mogrify("%s", (x, )) for x in buf['params'])))
    sql.commit()


def bufferMessageDeps(row, buf):
    messageId = row[0]['id']
    userProps = row[1]

    #("id", "subject", "body", "from", "to", "cc", "displayNames", "state", "date")
    params = (
        messageId,
        containsOr(row[0], 'subject', '')[:255].decode('utf-8', 'replace'),
        containsOr(row[0], 'body', ''),
        containsOr(row[0], 'from', '')[:36].decode('utf-8', 'replace'),
        containsOrNone(row[0], 'to', Json),
        containsOrNone(row[0], 'cc', Json),
        containsOrNone(row[0], 'displayNames', Json),
        row[0]['state'],
        row[0]['date'])
    if 'msgParams' not in buf.keys():
        buf['msgParams'] = []
    buf['msgParams'].append(params)

    for user in userProps:
        attachmentIds = containsOr(user, 'attachmentsIds', [])
        if attachmentIds is None:
            attachmentIds = []

        #(user_id, message_id, folder_id, trashed, unread, total_quota)
        params = (user['userId'][:36].decode('utf-8', 'replace'), messageId, user['folderId'], user['trashed'], user['unread'], user['totalSize'])
        if 'userMsgParams' not in buf.keys():
            buf['userMsgParams'] = []
        buf['userMsgParams'].append(params)

        for attId in attachmentIds:
            #(user_id, message_id, attachment_id)
            if 'userMsgAttParams' not in buf.keys():
                buf['userMsgAttParams'] = []
            buf['userMsgAttParams'].append((user['userId'][:36].decode('utf-8', 'replace'), messageId, attId))


def insertMessageDeps(buf):
    cur.execute(insertMessageQuery + (','.join(cur.mogrify("%s", (x, )) for x in buf['msgParams'])))
    if 'userMsgParams' in buf.keys():
        cur.execute(insertUserMessageQuery + (','.join(cur.mogrify("%s", (x, )) for x in buf['userMsgParams'])))
    if 'userMsgAttParams' in buf.keys():
        cur.execute(insertUserMessagesAttachmentQuery + (','.join(cur.mogrify("%s", (x, )) for x in buf['userMsgAttParams'])))
    sql.commit()


def updateParentMessage(row, buf):
    #("parent_id", "id")
    params = (row[1], row[0])
    cur.execute(updateParentMessageQuery['execute'], params)


def bufferAttachments(row, buf):
    # ("id", "name" , "charset" , "filename" , "contentType" , "contentTransferEncoding" , "size", "id")
    attachment = row[0]
    params = (attachment['id'], attachment['name'], attachment['charset'], attachment['filename'], attachment['contentType'], attachment['contentTransferEncoding'], attachment['size'])
    if 'params' not in buf.keys():
        buf['params'] = []
    buf['params'].append(params)


def insertAttachments(buf):
    cur.execute(insertAttachmentQuery + (','.join(cur.mogrify("%s", (x, )) for x in buf['params'])))
    sql.commit()


# CONSISTENCY #
###############

def consistencyChecks(*checks):
    for check in checks:
        consistencyCheck(check)


def consistencyCheck(checkObj):
    neoResults = gdb.query(checkObj['neo'], {}, returns=(convert))
    if len(neoResults) < 0:
        print('[ERROR][' + checkObj.text + '] Neo4j results length < 0')
    else:
        cur.execute(checkObj['sql'])

        neoRow = neoResults[0]
        sqlRow = cur.fetchone()
        for idx, neoCount in enumerate(neoRow):
            sqlCount = sqlRow[idx]

            if neoCount != sqlCount:
                print('[MISMATCH][' + checkObj['text'] + '] Neo4j count : (' + str(neoCount) + ') / Sql count : (' + str(sqlCount) + ')')
            else:
                print('[OK][' + checkObj['text'] + '] Count : (' + str(sqlCount) + ')')

# PROCESS #
###########

print('(' + str(datetime.now()) + ') ** Starting conversation DB migration process **')
print('Loop limit : ' + str(limit))

try:
    # Folders
    print('(' + str(datetime.now()) + ') [Start] Folders')
    results = gdb.query(foldersQuery, {}, returns=(lambda x: convert(x)['data'], convert, convert, convert, convert))
    buf = {}
    if len(results) > 0:
        for row in results:
            bufferFolder(row, buf)
        insertFolder(buf)
    print('(' + str(datetime.now()) + ') [Done]  Folders')

    # Attachments
    print('(' + str(datetime.now()) + ') [Start] Message attachments')
    pageLoop(attachmentsQuery, {}, bufferAttachments, loopAction=insertAttachments, returnList=(lambda x: convert(x)['data']))
    print('(' + str(datetime.now()) + ') [Done]  Message attachments')

    # Messages & all linked tables
    print('(' + str(datetime.now()) + ') [Start] Messages & dependencies')
    pageLoop(msgQuery, {}, bufferMessageDeps, loopAction=insertMessageDeps, returnList=(lambda x: convert(x)['data'], convert))
    print('(' + str(datetime.now()) + ') [Done]  Messages & dependencies')

    # Adding parent messages
    print('(' + str(datetime.now()) + ') [Start] Parent messages')
    cur.execute(updateParentMessageQuery['prepare'])
    pageLoop(msgParentQuery, {}, updateParentMessage, loopAction=commit, returnList=(convert, convert))
    print('(' + str(datetime.now()) + ') [Done]  Parent messages')

    # Consistency checks
    consistencyChecks(
        msgCheck,
        attCheck,
        foldersCheck,
        userFoldersCheck,
        usersMsgCheck,
        usersMsgAttCheck)

    print('(' + str(datetime.now()) + ') ** Conversation DB migration done **')

except psycopg2.Error as e:
    sys.stderr.write(e.pgerror)
    sys.stderr.write(e.pgcode)
    sys.stderr.flush()
    errorcodes.lookup(e.pgcode[:2])
    errorcodes.lookup(e.pgcode)
    traceback.print_exc()
except Exception as e:
    sys.stderr.write(str(e))
    sys.stderr.flush()
    traceback.print_exc()

cur.close()
sql.close()

