// Add ttl index on traces. Expire after 1 year
db.traces.createIndex({"entry": 1},{expireAfterSeconds: 31536000});