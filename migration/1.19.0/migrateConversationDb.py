#!/bin/env python
import sys
import traceback
import psycopg2
from psycopg2.extras import Json
from psycopg2 import errorcodes
from neo4jrestclient.client import GraphDatabase

gdb = GraphDatabase("http://localhost:7474/db/data/")
sql = psycopg2.connect(database="ong", host="localhost", user="web-education", password="We_1234")
sql.autocommit = False
cur = sql.cursor()

limit = 25

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
MATCH (msg:ConversationMessage)<-[mLink:HAS_CONVERSATION_MESSAGE]-(f:ConversationSystemFolder)<-[:HAS_CONVERSATION_FOLDER]-(c:Conversation)
OPTIONAL MATCH (msg)-[iLink:INSIDE]->(uf:ConversationUserFolder)<-[:HAS_CHILD_FOLDER*0..5]-()<-[:HAS_CONVERSATION_FOLDER]-(c)
OPTIONAL MATCH (uma:MessageAttachment)<-[:HAS_ATTACHMENT]-(msg)<-[mLink]-(f)
WHERE uma.id IN mLink.attachments
WITH DISTINCT msg, c, uf, collect(f.name) as folderNames, count(mLink.unread) as unreadFlag,
mLink.attachments as attachments,
iLink, sum(uma.size) as totalSize
WITH msg, collect(DISTINCT {
    userId: c.userId,
    folderId: uf.id,
    attachmentsIds: attachments,
    totalSize: totalSize,
    unread: coalesce(unreadFlag = 0, true),
    trashed: coalesce(iLink.trashed OR "TRASH" IN folderNames, false)
}) as userProps
RETURN msg, userProps
SKIP {skip} LIMIT {limit};
"""

msgParentQuery = """
MATCH (msg:ConversationMessage)-[:PARENT_CONVERSATION_MESSAGE]->(pMsg:ConversationMessage)
WITH msg, pMsg.id as parent_id
RETURN msg.id, parent_id
SKIP {skip} LIMIT {limit};
"""

# POSTRGESQL QUERIES #
######################

insertFolders = """
INSERT INTO conversation.folders
    (id, parent_id, user_id, name, depth, trashed)
VALUES
    (%s, %s, %s, %s, %s, %s)
"""
upsertAttachment = """
INSERT INTO conversation.attachments
    (id, name, charset, filename, "contentType", "contentTransferEncoding", size)
VALUES
    (%s, ' ', ' ', ' ', ' ', ' ', 0)
ON CONFLICT DO NOTHING
"""
updateAttachmentQuery = """
UPDATE conversation.attachments
SET "name" = %s, "charset" = %s, "filename" = %s, "contentType" = %s, "contentTransferEncoding" = %s, "size" = %s
WHERE id = %s
"""
insertMessage = """
INSERT INTO conversation.messages
    ("id", "subject", "body", "from", "to", "cc", "displayNames", "state", "date")
VALUES
    (%s, %s, %s, %s, %s, %s, %s, %s, %s)
"""
updateParentMessageQuery = """
UPDATE conversation.messages
SET parent_id = %s
WHERE id = %s
"""
insertUserMessage = """
INSERT INTO conversation.usermessages
    (user_id, message_id, folder_id, trashed, unread, total_quota)
VALUES
    (%s, %s, %s, %s, %s, %s)
"""
insertUserMessagesAttachment = """
INSERT INTO conversation.usermessagesattachments
    (user_id, message_id, attachment_id)
VALUES
    (%s, %s, %s)
"""

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
    MATCH (a:MessageAttachment)<-[:HAS_ATTACHMENT]-(cm:ConversationMessage)-[mLink:HAS_CONVERSATION_MESSAGE]-()
    WHERE a.id IN mLink.attachments
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


def commit():
    sql.commit()


# PAGINATION LOOP #
###################

def pageLoop(query, params, rowAction, endAction=(lambda: ()), returnList=()):
    stop = False
    page = 0
    while not stop:
        paramsCopy = params.copy()
        paramsCopy.update({'skip': page * limit, 'limit': limit})
        results = gdb.query(query, paramsCopy, returns=returnList)
        if len(results) > 0:
            for row in results:
                rowAction(row)
            page = page + 1
        else:
            stop = True
    endAction()


# QUERY ACTIONS #
#################

def insertFolder(row):
    #(id, parent_id, user_id, name, depth, trashed)
    params = (row[0]['id'], row[1], row[2], row[0]['name'], row[3], row[4])
    cur.execute(insertFolders, params)


def insertMessageDeps(row):
    messageId = row[0]['id']
    userProps = row[1]

    #("id", "subject", "body", "from", "to", "cc", "displayNames", "state", "date")
    params = (
        messageId,
        containsOr(row[0], 'subject', ''),
        row[0]['body'],
        row[0]['from'],
        containsOrNone(row[0], 'to', Json),
        containsOrNone(row[0], 'cc', Json),
        containsOrNone(row[0], 'displayNames', Json),
        row[0]['state'],
        row[0]['date'])
    cur.execute(insertMessage, params)

    for user in userProps:
        attachmentIds = containsOr(user, 'attachmentsIds', [])
        if attachmentIds is None:
            attachmentIds = []

        for attId in attachmentIds:
            #(id)
            cur.execute(upsertAttachment, [attId])

        #(user_id, message_id, folder_id, trashed, unread, total_quota)
        params = (user['userId'], messageId, user['folderId'], user['trashed'], user['unread'], user['totalSize'])
        cur.execute(insertUserMessage, params)

        for attId in attachmentIds:
            #(user_id, message_id, attachment_id)
            cur.execute(insertUserMessagesAttachment, (user['userId'], messageId, attId))


def updateParentMessage(row):
    #("parent_id", "id")
    params = (row[1], row[0])
    cur.execute(updateParentMessageQuery, params)


def updateAttachment(row):
    # ("name" , "charset" , "filename" , "contentType" , "contentTransferEncoding" , "size", "id")
    attachment = row[0]
    params = (attachment['name'], attachment['charset'], attachment['filename'], attachment['contentType'], attachment['contentTransferEncoding'], attachment['size'], attachment['id'])
    cur.execute(updateAttachmentQuery, params)


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

try:
    # Folders
    results = gdb.query(foldersQuery, {}, returns=(lambda x: convert(x)['data'], convert, convert, convert, convert))
    if len(results) > 0:
        for row in results:
            insertFolder(row)
        sql.commit()

    # Messages & all linked tables
    pageLoop(msgQuery, {}, insertMessageDeps, commit, returnList=(lambda x: convert(x)['data'], convert))

    # Adding parent messages
    pageLoop(msgParentQuery, {}, updateParentMessage, commit, returnList=(convert, convert))

    # Attachments completion
    pageLoop(attachmentsQuery, {}, updateAttachment, commit, returnList=(lambda x: convert(x)['data']))

    # Consistency checks
    consistencyChecks(
        msgCheck,
        attCheck,
        foldersCheck,
        userFoldersCheck,
        usersMsgCheck,
        usersMsgAttCheck)

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
