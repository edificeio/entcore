#!/bin/bash
mongo localhost:27017/one_gridfs cleanAndSetDocumentsSizes.js
mongo localhost:27017/one_gridfs calcStorageSizeByUser.js > /tmp/setStorageSizeByUser.cypher
more +3 /tmp/setStorageSizeByUser.cypher > storageSizeByUser.cypher
neo4j-shell < setQuotas.cypher
neo4j-shell < storageSizeByUser.cypher

