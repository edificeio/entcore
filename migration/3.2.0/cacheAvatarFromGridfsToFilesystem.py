#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os
import unicodedata
from neo4jrestclient.client import GraphDatabase
from pymongo import MongoClient
import shutil
import errno
import gridfs

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise
            
def substring_after(s, delim):
    return s.split(delim)[-1]

def prepare_dir(basePath, fileName, makeDir = 0):
    dir1 = fileName[-2:]
    dir2 = fileName[-4:-2]
    dirPath = os.path.join(basePath,dir1,dir2)
    print "Trying to create directory %s" % (dirPath)
    if makeDir==1: mkdir_p(dirPath)
    return os.path.join(dirPath,fileName)

def save_file(fs,fileId, basePath, fileName):
    fileSrc = fs.get(fileId)
    fileDestPath = prepare_dir(basePath,fileName,1)
    print "Start writing (%s) written to %s" % (fileId,fileDestPath)
    fileDest = open(fileDestPath,"w")
    fileDest.write(fileSrc.read())
    fileDest.close()
    print "File (%s) written to %s" % (fileId,fileDest)
    
def cp_noavatar(basePath):
    fileSrc = os.path.abspath("./no-avatar.png")
    fileDest = prepare_dir(basePath,"no-avatar",1)
    shutil.copy(fileSrc,fileDest)
    print "Default file written from %s to %s" % (fileSrc,fileDest)

def normalize_path(path):
	path = os.path.expanduser(path)
	return path if os.path.isabs(path) else os.path.abspath(path)

if len(sys.argv) == 5:
    gdb = GraphDatabase("http://%s:7474/db/data/" % sys.argv[1])
    mdb = MongoClient("mongodb://%s:27017/" % sys.argv[2])
    fs = gridfs.GridFS(mdb)
    documents = mdb[sys.argv[3]][sys.argv[4]]
    avatarPath = normalize_path(sys.argv[5])
    cp_noavatar(avatarPath)
    query = "MATCH (u:UserBook) WHERE u.picture =~ '/workspace/document/.*' WITH u, u.picture as oldpic SET u.picture='/userbook/avatar/'+u.userid RETURN u.userid as userid, oldpic"
    res = gdb.query(query, {}, returns=(unicode, unicode, unicode))
    if res:
        for row in res:
            userid = row[0]
            pictureId = substring_after(row[1],os.path.sep)
            document = documents.find_one({"_id": pictureId})
            #save main avatar       
            fileId = document["file"]
            fileName = userid
            save_file(fs,fileId,avatarPath,fileName)
             #save thumbnails     
            for size in document["thumbnails"]:
                fileId = document["thumbnails"][size]
                fileName = "%s-%s" % (size,userid)
                save_file(fs,fileId,avatarPath,fileName)    
else:
    print("bad arguments: neo4j_host mongo_host mongo_dbname mongo_collection avatar_path")