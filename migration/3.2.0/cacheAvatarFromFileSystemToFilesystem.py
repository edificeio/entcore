#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import os
import unicodedata
from neo4jrestclient.client import GraphDatabase
from pymongo import MongoClient
import shutil
import errno
import subprocess
logError = open('cache-avatar.error.txt', 'w')

def make_copy(source,dest, fast=1):
    if fast==1:
        cmd=['cp', source, dest] if (sys.platform.startswith("darwin") or sys.platform.startswith("linux")) else ['xcopy', source, dest, '/K/O/X']
        proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)       
        code = proc.wait()
        if code != 0:
            logError.write("err_save_file;%s;%s;%s;%s\n" % (code, "",source,dest))
    else:
        try:
            shutil.copy(source,dest)
            #print "File (%s) written from %s to %s" % (fileId,fileSrc,fileDest)
        except IOError as e:
            logError.write("err_save_file;%s;%s;%s;%s\n" % (e.errno, e.strerror,source,dest))

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

def save_file(workspacePath,fileId, basePath, fileName):
    fileSrc = prepare_dir(workspacePath,fileId)
    fileDest = prepare_dir(basePath,fileName,1)
    #print "Start writing (%s) written from %s to %s" % (fileId,fileSrc,fileDest)
    make_copy(fileSrc,fileDest)
    #print "File (%s) written from %s to %s" % (fileId,fileSrc,fileDest)
    
def cp_noavatar(basePath):
    fileSrc = os.path.abspath("./no-avatar.png")
    fileDest = prepare_dir(basePath,"no-avatar",1)
    make_copy(fileSrc,fileDest)
    print "Default file written from %s to %s" % (fileSrc,fileDest)

def normalize_path(path):
	path = os.path.expanduser(path)
	return path if os.path.isabs(path) else os.path.abspath(path)

if len(sys.argv) >= 7:
    gdb = GraphDatabase("http://%s:7474/db/data/" % sys.argv[1])
    mdb = MongoClient("mongodb://%s:27017/" % sys.argv[2])
    documents = mdb[sys.argv[3]][sys.argv[4]]
    workspacePath = normalize_path(sys.argv[5])
    avatarPath = normalize_path(sys.argv[6])
    batchSize = sys.argv[7] if len(sys.argv) > 7 else 10000
    cp_noavatar(avatarPath)
    query = """MATCH (u:UserBook) WHERE u.picture =~ '/workspace/document/.*' 
               WITH u, u.picture as oldpic 
               LIMIT %s
               SET u.picture='/userbook/avatar/' + u.userid , u.oldPicture=oldpic
               RETURN u.userid as userid, oldpic""" % (batchSize)
    res = gdb.query(query, {}, returns=(unicode, unicode, unicode))
    batchNum = 0
    while res and len(res) > 0:
        batchNum += 1
        print "Processing batch number %s (%s rows)" % (batchNum,len(res))
        for row in res:
            userid = row[0]
            pictureId = substring_after(row[1],os.path.sep)
            document = documents.find_one({"_id": pictureId})
            #save main avatar  
            if document and document.has_key("file"):       
                fileId = document["file"]
                fileName = userid
                save_file(workspacePath,fileId,avatarPath,fileName)
                #save thumbnails     
                for size in document.get("thumbnails",dict()):
                    fileId = document["thumbnails"][size]
                    fileName = "%s-%s" % (size,userid)
                    save_file(workspacePath,fileId,avatarPath,fileName) 
        res = gdb.query(query, {}, returns=(unicode, unicode, unicode))
    print "Finished to process %s batchs" % batchNum
    logError.close()
else:
    print("bad arguments: neo4j_host mongo_host mongo_dbname mongo_collection workspace_path avatar_path [batch_size]")

