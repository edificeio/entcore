
var startTime, endTime;
function startMeasure(name) {
    print("event;start measuring;" + name);
    startTime = new Date();
}
function endMeasure(name) {
    endTime = new Date();
    var timeDiffMs = endTime - startTime; //in ms
    var timeDiffSeconds = (timeDiffMs / 1000);
    var timeDiffMinutes = timeDiffSeconds / 60;
    var seconds = Math.round(timeDiffSeconds);
    var minutes = Math.round(timeDiffMinutes);
    print("event;end measuring:" + minutes + " minutes (" + seconds + "seconds);" + name);
}

var counter = 0;
var buffer = [];
var BULK_SIZE = 10000;
startMeasure("begin")

function save() {
    if (buffer.length) {
        var bulk = db.timelineMobile.initializeUnorderedBulkOp();
        buffer.forEach(function (event) {
            bulk.insert(event);
        });
        bulk.execute();
    }
    buffer = [];
    print("Nb inserted :" + counter);
}

db.timeline.find({ preview: { $exists: true } }).forEach(function (event) {
    buffer.push(event);
    counter++;
    if (counter % BULK_SIZE == 0) {
        save();
    }
});
save();
endMeasure("end")

db.timelineMobile.createIndex({
    "reporters.userId": 1
}, {
    "name": "reporters.userId_1",
    "background": true
});
db.timelineMobile.createIndex({
    "reporters.date": -1
}, {
    "name": "reporters.date_-1",
    "background": true
});

db.timelineMobile.createIndex({
    "reportAction": 1
}, {
    "name": "reportAction_1",
    "background": true
})
db.timelineMobile.createIndex({
    "reportAction.date": -1
}, {
    "name": "reportAction.date_-1",
    "background": true
});
db.timelineMobile.createIndex({
    "sender": 1
}, {
    "name": "sender_1",
    "background": true
});
db.timelineMobile.createIndex({
    "type": 1
}, {
    "name": "type_1",
    "background": true
});
db.timelineMobile.createIndex({
    "recipients.userId": 1,
    "created": -1
}, {
    "name": "recipients.userId_1_created_-1",
    "background": true
});
db.timelineMobile.createIndex({
    "date": 1,
    "sender": 1,
    "created": -1
}, {
    "name": "date_1_sender_1_created_-1",
    "background": true
});
db.timelineMobile.createIndex({
    "sender": 1,
    "created": -1
}, {
    "name": "sender_1_created_-1",
    "background": true
});
