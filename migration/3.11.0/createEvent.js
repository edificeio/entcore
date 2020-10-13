const { Client, Pool } = require('pg')
const Cursor = require('pg-cursor')
const neo4j = require('neo4j');
const date = require('date-and-time');
const { v4: uuidv4 } = require('uuid');
const objectPath = require("object-path");
const MongoClient = require('mongodb').MongoClient;
const Long = require('mongodb').Long;
const yargs = require('yargs');
const { promisify } = require('util')
Cursor.prototype.readAsync = promisify(Cursor.prototype.read)

const argv = yargs
    .option("from-date", {
        alias: "fd",
        description: "Date from wich we compute events",
        type: "string"
    })
    .option('mongo-address', {
        alias: 'ma',
        description: 'Mongo DB address',
        type: 'string',
        default: "localhost"
    })
    .option('mongo-collection', {
        alias: 'mc',
        description: 'Mongo DB address',
        type: 'string',
        default: "ent"
    })
    .option('neo4j-address', {
        alias: 'na',
        description: 'NEO4J address',
        type: 'string',
        default: "localhost"
    })
    .option('postgres-address', {
        alias: 'pa',
        description: 'Postgres address',
        type: 'string',
        default: "localhost"
    })
    .option('postgres-port', {
        alias: 'pp',
        description: 'Postgres port',
        type: 'number',
        default: 5432
    })
    .option('postgres-db', {
        alias: 'pdb',
        description: 'Postgres database',
        type: 'string',
        default: "ent"
    })
    .option('postgres-user', {
        alias: 'pu',
        description: 'Postgres user',
        type: 'string',
        default: ""
    })
    .option('postgres-password', {
        alias: 'pw',
        description: 'Postgres password',
        type: 'string',
        default: ""
    })
    .option('stats-postgres-address', {
        alias: 'spa',
        description: 'Postgres Stat address',
        type: 'string',
        default: "localhost"
    })
    .option('stats-postgres-port', {
        alias: 'spp',
        description: 'Postgres Stat port',
        type: 'number',
        default: 5432
    })
    .option('stats-postgres-db', {
        alias: 'spdb',
        description: 'Postgres Stat database',
        type: 'string',
        default: ""
    })
    .option('stats-postgres-user', {
        alias: 'spu',
        description: 'Postgres Stat user',
        type: 'string',
        default: ""
    })
    .option('stats-postgres-password', {
        alias: 'spw',
        description: 'Postgres Stat password',
        type: 'string',
        default: ""
    })
    .option('stats-platformid', {
        alias: 'spl',
        description: 'Postgres Stat platformid',
        type: 'string',
        default: ""
    })
    .option('batch-size', {
        alias: 'bs',
        description: 'Batch size of insert event',
        type: 'number',
        default: 10000
    })
    .option('config-file', {
        alias: 'cf',
        description: 'JS Config file containing list of apps',
        type: 'string',
        default: 'createEventConf.js'
    })
    .help()
    .alias('help', 'h')
    .argv;

const configs = require((process.cwd()+'/') + (argv["config-file"] || "createEventConf.js"));

// Connection URL
const url = `mongodb://${argv["mongo-address"] || 'localhost'}:27017`;
const dbName = argv["mongo-collection"] || 'ent';
const collName = "events";
// Use connect method to connect to the server
var total = 0;
async function execute() {
    const startAt = new Date();
    try {
        console.log("Start at : ", startAt);
        const neo4jData = await loadStructure();
        const client = await MongoClient.connect(url)
        const sqlPool = new Pool({
            user: argv["postgres-user"] || null,
            host: argv["postgres-address"] || 'localhost',
            database: argv['postgres-db'] || 'ent',
            password: argv["postgres-password"] || null,
            port: argv["postgres-port"] || 5432,
        })
        const sqlClient = await sqlPool.connect();
        console.log("Connected successfully to server");
        const db = client.db(dbName);
        const dateParsed = date.parse(argv["from-date"], 'DDMMYYYY');
        for (const config of configs) {
            try {
                console.log("Starting resource: ", config.resourceType)
                var count = 0;
                const module = config.module;
                const resourceType = config.resourceType;
                var nbSub = 0;
                if (config.mongodb) {
                    const bulk = []
                    const criteria = config.criteria || {};
                    const createdField = config.dateField || "created";
                    const criteriaWithDate = { ...criteria, [createdField]: { $gte: config.dateFieldString ? dateParsed.toISOString() : dateParsed } };
                    console.log(JSON.stringify(criteriaWithDate))
                    const cursor = db.collection(config.collection).find(criteriaWithDate, { batchSize: getBatchSize() });
                    while (await cursor.hasNext()) {
                        try {
                            const doc = await cursor.next();
                            count += bulk.length;
                            await partialWriteEvents(bulk);
                            const resourceId = doc._id;
                            var created = objectPath.get(doc, config.dateField || "created");
                            var ownerId = objectPath.get(doc, config.ownerField || "owner");
                            const data = getUser(neo4jData, ownerId, config.searchByName);
                            if (config.searchByName) {
                                ownerId = data.userId;
                            }
                            if (config.dateFieldFormat) {
                                created = date.parse(created, config.dateFieldFormat);
                            }
                            bulk.push({
                                "_id": uuidv4(),
                                "migrationId": `${module}_${resourceType}_${resourceId}`,
                                "resource-type": resourceType,
                                "event-type": "CREATE",
                                "module": module,
                                "date": new Date(created),
                                "userId": ownerId,
                                "profil": data["profile"],
                                "structures": data["structuresIds"],
                                "classes": data["classesIds"],
                                "groups": data["groupsIds"],
                                "ua": ""
                            })
                            if (config.subResources) {
                                const keys = Object.keys(config.subResources)
                                for (var key of keys) {
                                    const subArray = objectPath.get(doc, key);
                                    const subResource = config.subResources[key];
                                    if (subArray instanceof Array) {
                                        for (var sub of subArray) {
                                            const subOwnerId = subResource.inheritsOwner ? ownerId : objectPath.get(sub, subResource.ownerField || config.ownerField || "owner");
                                            const subData = neo4jData[subOwnerId] || { structuresIds: [], groupsIds: [], classesIds: [], profile: "" };
                                            var subCreated = objectPath.get(sub, subResource.dateField || config.dateField || "created");
                                            if (subResource.dateFieldFormat || config.dateFieldFormat) {
                                                subCreated = date.parse(subCreated, subResource.dateFieldFormat || config.dateFieldFormat);
                                            }
                                            bulk.push({
                                                "_id": uuidv4(),
                                                "migrationId": `${module}_${resourceType}_${subResource.resourceType}_${resourceId}`,
                                                "resource-type": subResource.resourceType,
                                                "event-type": "CREATE",
                                                "module": module,
                                                "date": new Date(subCreated),
                                                "userId": subOwnerId,
                                                "profil": subData["profile"],
                                                "structures": subData["structuresIds"],
                                                "classes": subData["classesIds"],
                                                "groups": subData["groupsIds"],
                                                "ua": ""
                                            })
                                        }
                                        nbSub += subArray.length;
                                    }
                                }
                            }
                        } catch (e) {
                            console.error("failed to parse element: ", e)
                        }
                    }
                    count += bulk.length;
                    total += count;
                    //console.log("Sub resource created : ", resourceType, nbSub)
                    //console.log("Resource created : ", resourceType, count)
                    await writeEvents(bulk);
                } else {//postgres
                    const createdField = config.dateField || "created";
                    var createdValue = config.dateFieldString ? dateParsed.toISOString() : dateParsed;
                    if (config.dateFieldInt) {
                        createdValue = dateParsed.getTime()
                    }
                    const wheres = [`${createdField} >= '${createdValue}'`]
                    if (config.where) {
                        wheres.push(config.where)
                    }
                    const query = `SELECT * FROM ${config.table} WHERE  ${wheres.join(" AND ")}`;
                    //console.log(query);
                    const cursor = sqlClient.query(new Cursor(query));
                    const bulk = [];
                    var rows = await cursor.readAsync(getBatchSize())
                    while (rows.length) {
                        for (var row of rows) {
                            await partialWriteEvents(bulk);
                            const resourceId = row.id;
                            var created = objectPath.get(row, config.dateField || "created");
                            var ownerId = objectPath.get(row, config.ownerField || "owner");
                            const data = getUser(neo4jData, ownerId, config.searchByName);
                            if (config.searchByName) {
                                ownerId = data.userId;
                            }
                            if (config.dateFieldFormat) {
                                created = date.parse(created, config.dateFieldFormat);
                            }
                            if (config.dateFieldInt) {
                                created = parseInt(created, 10)
                            }
                            bulk.push({
                                "_id": uuidv4(),
                                "migrationId": `${module}_${resourceType}_${resourceId}`,
                                "resource-type": resourceType,
                                "event-type": "CREATE",
                                "module": module,
                                "date": new Date(created),
                                "userId": ownerId,
                                "profil": data["profile"],
                                "structures": data["structuresIds"],
                                "classes": data["classesIds"],
                                "groups": data["groupsIds"],
                                "ua": ""
                            })
                        }
                        rows = await cursor.readAsync(getBatchSize())
                    }
                    count = bulk.length;
                    total += count;
                    await writeEvents(bulk);
                }
                console.log("Finish success resource: ", config.resourceType, count)
            } catch (e) {
                console.error("Finish fail: " + config.resourceType + " (" + count + ")", e)
            }
        }
        client.close();
        console.log("Nb events created : ", total)
    } catch (e) {
        console.error(e);
    } finally {
        console.log("Finished at :", new Date(), startAt)
    }
}
execute();

function getUser(neo4jData, ownerId, byName) {
    if (byName) {
        for (var i in neo4jData) {
            var user = neo4jData[i];
            if (user.displayName == ownerId) {
                return user;
            }
        }
    }
    return neo4jData[ownerId] || { structuresIds: [], groupsIds: [], classesIds: [], profile: "" }
}

function loadStructure() {
    if (saveStatInPostgres()) {
        return new Promise((resolve, reject) => {
            const query = `MATCH (u:User) 
                        WHERE HAS(u.login) 
                        RETURN distinct 
                        u.id as userId, 
                        u.displayName as displayName,
                        HEAD(u.profiles) as profile
            `;
            const db = new neo4j.GraphDatabase(`http://${argv["neo4j-address"]}:7474`);
            db.cypher(query, (err, results) => {
                if (err) {
                    reject(err);
                    return;
                }
                var data = {};
                for (var res of results) {
                    res.classesIds = [];
                    res.groupsIds = [];
                    res.structuresIds = [];
                    data[res.userId] = res;
                }
                resolve(data)
            });
        })
    } else {
        return new Promise((resolve, reject) => {
            const query = `MATCH (u:User) 
                        WHERE HAS(u.login) 
                        OPTIONAL MATCH (u)-[:IN]->(gp:Group) 
                        OPTIONAL MATCH (gp:ProfileGroup)-[:DEPENDS]->(s:Structure) 
                        OPTIONAL MATCH (gp)-[:DEPENDS]->(c:Class) 
                        RETURN distinct 
                        u.id as userId, 
                        u.displayName as displayName,
                        HEAD(u.profiles) as profile,
                        COLLECT(distinct c.id) as classesIds, 
                        COLLECT(distinct gp.id) as groupsIds,
                        COLLECT(distinct s.id) as structuresIds
            `;
            const db = new neo4j.GraphDatabase(`http://${argv["neo4j-address"]}:7474`);
            db.cypher(query, (err, results) => {
                if (err) {
                    reject(err);
                    return;
                }
                var data = {};
                for (var res of results) {
                    data[res.userId] = res;
                }
                resolve(data)
            });
        })
    }
}

var nbWrite = 0;

function getBatchSize() {
    return argv["batch-size"] || 10000;
}

function saveStatInPostgres() {
    return argv["stats-postgres-db"];
}

async function partialWriteEvents(bulks) {
    var BATCH_SIZE = getBatchSize();
    if (BATCH_SIZE <= bulks.length) {
        await writeEvents(bulks);
        const tmp = bulks.length;
        bulks.length = 0;
        console.log("Writed batch of: ", tmp, "now: ", bulks.length)
    }
}

async function writeEvents(bulks) {
    if (bulks.length > 0) {
        try {
            if (saveStatInPostgres()) {
                if (nbWrite == 0) {
                    console.log("Save stats into postgres....")
                }
                const sql = new Pool({
                    user: argv["stats-postgres-user"] || null,
                    host: argv["stats-postgres-address"] || 'localhost',
                    database: argv['stats-postgres-db'],
                    password: argv["stats-postgres-password"] || null,
                    port: argv["stats-postgres-port"] || 5432,
                })
                const client = await sql.connect();
                try {
                    await client.query('BEGIN')
                    const futures = bulks.map(e => {
                        try{
                            return client.query({
                                text: `INSERT INTO events.create_events (id,date,profile,module,event_type,ua,platform_id,user_id,resource_type,migration_id) 
                                                                VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
                                values: [e._id, e.date.toISOString(), e.profil, e.module, e["event-type"], e.ua, argv["stats-platformid"], e.userId, e["resource-type"], e.migrationId]
                            });
                        }catch(e){
                            //console.error(e)
                            return null;
                        }
                    }).filter(e => e != null);
                    await Promise.all(futures);
                    await client.query('COMMIT')
                } catch (e) {
                    console.error("postgres failed :", e)
                    await client.query('ROLLBACK')
                    throw e
                } finally {
                    client.release()
                }
            } else {
                if (nbWrite == 0) {
                    //create migration index
                    try {
                        await db.collection(collName).createIndex("migrationId", { unique: true });
                    } catch (e) {
                        console.error(e);
                    }
                    console.log("Save stats into mongo....")
                }
                const realBulk = bulks.map(e => {
                    e.date = Long.fromNumber(e.date.getTime());
                    return {
                        insertOne: {
                            document: e
                        }
                    }
                })
                const client = await MongoClient.connect(url)
                const db = client.db(dbName);
                await db.collection(collName).bulkWrite(realBulk);
            }
        } finally {
            nbWrite++;
        }
    }
}