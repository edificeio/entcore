
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