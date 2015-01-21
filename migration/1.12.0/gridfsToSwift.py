#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pymongo
import gridfs
import swiftclient

# change swift parameters
user = 'test:tester'
key = 'testing'
authurl = 'http://172.17.0.2:8080/auth/v1.0'
container_name = 'my-new-container'

conn = pymongo.Connection()
db = conn.one_tests # Change db name
fs = gridfs.GridFS(db)

swift = swiftclient.Connection(
    user=user,
    key=key,
    authurl=authurl
)

swift.put_container(container_name)

for grid_out in fs.find({}, timeout=False):
    _id = grid_out._id
    filename = grid_out.filename
    content_type = grid_out.content_type
    length = grid_out.length
    data = grid_out.read()
    swift.put_object(container_name, _id, data, content_length=length, content_type=content_type, headers={'X-Object-Meta-Filename': filename})

