// Compute the elapsed time between 2 javascript Date
function calc_duration(from, to) {
    var timeDiffMs = to - from; //in ms
    var timeDiffSeconds = (timeDiffMs / 1000);
    var timeDiffMinutes = timeDiffSeconds / 60;
    var seconds = Math.round(timeDiffSeconds);
    var minutes = Math.round(timeDiffMinutes);
    return ""+minutes+"m"+seconds+"s";
}

// Iterate through small batches of documents and delete them.
function purge(collection, template, batchSize) {
    if( batchSize <= 0 ) return;
    var startTime = new Date();
    var totalDeleted = 0;
    do {
        var ids = db[collection].find(template,{_id:1}).limit(batchSize).toArray().map( function(item){return item._id;} );
        if( ids.length ) {
            db[collection].deleteMany( {_id: {$in: ids}} );
            totalDeleted += ids.length;
        }
    } while( ids.length >= batchSize );
    print( ""+totalDeleted+" documents deleted from "+collection+" in "+calc_duration(startTime, new Date()) );
}

// delete old MOOD notifications
purge( "timeline", {"type":"USERBOOK", "event-type":"USERBOOK_MOOD"}, 100 );

// delete old MOTTO notifications
purge( "timeline", {"type":"USERBOOK", "event-type":"USERBOOK_MOTTO"}, 100 );