//===Measure utils
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
//===Utils
String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
String.prototype.getExtension = function(){
    return this.substr(this.lastIndexOf('.') + 1);
}
function removeLastPoint(str){
    return str.replace(/\.$/, "");
}
var forbiddenExtension = ["ade","and","adp","ani","asp","asx","bas","bat","cab","cga","cgb","cgc","cgd","cge","cgf","cgg","cgh","cgi","cgj","cgk","cgl","cgm","cgn","cgo","cgp","cgq","cgr","cgs","cgt","cgu","cgv","cgw","cgx","cgy","cgz","chk","chm","clp","cmd","cnf","cod","col","com","conf","config","cpio","cpl","cpp","crl","crt","csh","exe","grp","hhp","hlp","ht","hta","idx","inf","ins","isp","job","js","jse","lnk","mcw","mdb","mde","msc","msg","msi","mso","msp","msrcincident","msstyles","mst","pcd","pif","prf","rar","reg","sc2","scd","scf","sch","schema","scp","scr","sct","shb","shs","shtm","shtml","swf","url","vb","vbe","vbs","vxd","wab","wax","wbk","wcs","wdb","web","webinfo","webpnp","wht","wiz","wizhtml","wll","wm","wma","wmd","wmdb","wmf","wms","wmx","wmz","wpc","wpl","wpz","wri","wsc","wsdl","wsf","wsh","wtx","wvx"]
function ignoreExtension(ext){
    ext = (ext || "").replace(".","")
    return !ext || ext.trim().length==0 || forbiddenExtension.indexOf(ext) > -1;
}
//
var DOC_COLLECTION = "documents";
var context = {
    noExtension: 0,
    goodExtension: 0,
    skipped:[],
    bulk: [],
    toChange:[],
    ignoreExt:0,
    toIgnore:[]
}
var toUpdate = [];
startMeasure("all")
db[DOC_COLLECTION].find({eType:"file"}).forEach(function (document) {
    if(document.metadata && document.metadata.filename && document.name){
        var filename = document.metadata.filename.trim();
        var name = document.name.trim();
        var extension = "." + filename.getExtension();
        //if no extension in filename
        if(filename.indexOf(".")==-1){
            context.noExtension++;
        //if name contains a good extension or name already contains an extension => consider extension is good
        }else if(name.endsWith(extension) || name.indexOf(".")>-1){
            context.goodExtension++;
        //if bad extension => ignore (improve perf by setting this test after others)
        }else  if(ignoreExtension(extension)){
            context.ignoreExt++;
            context.toIgnore.push({_id:document._id, created: document.created,copyFromId:document.copyFromId, name:document.name,extension:extension,ownerName:document.ownerName, metadata: JSON.stringify(document.metadata)})
        //if name contains a good extension or name already contains an extension => consider extension is good
        }else{
            //ELSE RENAME
            var newname = removeLastPoint(name) + extension;
            context.toChange.push({_id:document._id, created: document.created,copyFromId:document.copyFromId, name:document.name,newname:newname,ownerName:document.ownerName, metadata: JSON.stringify(document.metadata)})
            context.bulk.push({
                "updateOne": {
                    "filter": { "_id": document._id },
                    "update": {
                        "$set": { 'fixExtension': true,  'name': newname}
                    }
                }
            })
        }
    }else{
        context.skipped.push({_id:document._id, created: document.created, name:document.name, metadata: JSON.stringify(document.metadata)})
    }
});
//BULK
startMeasure("bulk")
//print("dump----------------------------------------------")
//print(context.bulk)
//print("enddump----------------------------------------------")
db[DOC_COLLECTION].bulkWrite(context.bulk)
endMeasure("bulk")
//
endMeasure("all")
//
print("start report"+"\n")
print("-----------------------------------------------"+"\n")
print("list of skipped documents"+"\n")
context.skipped.forEach(function(report){
//    print(report._id + ";" + report.name + ";" + report.created)
})
print("-----------------------------------------------"+"\n")
print("list of ignored documents"+"\n")
context.toIgnore.forEach(function(report){
    print(report._id + ";" + report.name + ";" + report.created+";"+report.extension+";"+report.ownerName+";"+report.copyFromId)
})
print("-----------------------------------------------"+"\n")
print("list of changed documents"+"\n")
context.toChange.forEach(function(report){
    print(report._id + ";" + report.name + ";" + report.created+";"+report.newname+";"+report.ownerName+";"+report.copyFromId)
})
print("-----------------------------------------------"+"\n")
print("nb ignored extensions: " + context.ignoreExt+"\n")
print("nb skipped because of absent fields: " + context.skipped.length+"\n")
print("nb document with correct extension: " + context.goodExtension+"\n")
print("nb document without extension (but its ok): " + context.noExtension+"\n")
print("nb document changed: " + context.bulk.length+"\n")
print("end report")