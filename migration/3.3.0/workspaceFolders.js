var COLLECTION_NAME = "documents.recette";
var OLD_RIGHT = "org-entcore-workspace-service-WorkspaceService";
var NEW_RIGHT = "org-entcore-workspace-controllers-WorkspaceController";
var foldersByOwner = {}

function replaceShared(doc) {
    if (!doc.shared) {
        return;
    }
    for (var i = 0; i < doc.shared.length; i++) {
        var item = doc.shared[i];
        for (var j in item) {
            if (j.indexOf(OLD_RIGHT) == 0) {
                //TODO add new rights
                var replaced = j.replace(OLD_RIGHT, NEW_RIGHT);
                item[replaced] = item[j];
                delete item[j]
                //ADD NEW RIGHT
                if (j.indexOf("deleteComment")) {//if manager
                    item[NEW_RIGHT + "|bulkDelete"] = true;
                }
            }
        }
    }
}
function findParentFor(doc, parentPath) {
    if (!parentPath || parentPath == "media-library" || parentPath == "Trash") {
        return null;
    }
    var folders = foldersByOwner[doc.owner] || {};
    var founded = folders[parentPath] || null;
    if (founded == null) {
        print("error;could not found folder for document: " + doc._id + "/" + parentPath)
    }
    return founded;
}
function pushFolder(doc) {
    if (!foldersByOwner[doc.owner]) {
        foldersByOwner[doc.owner] = {};
    }
    foldersByOwner[doc.owner][doc.folder] = doc;
}
function isFile(doc) {
    return !!doc.file;
}
function isFolder(doc) {
    return !doc.file;
}
function getLastPath(doc) {
    if (doc.name.indexOf("_")) {
        var splits = doc.name.split("_");
        return splits[splits.length - 1]
    } else {
        return doc.name;
    }
}
function getAllButLastPath(doc) {
    if (doc.folder.indexOf("_")) {
        var splits = doc.folder.split("_");
        splits.pop()
        return splits.join("_");
    } else {
        return "";
    }
}
function hasShared(doc) {
    return doc.shared && doc.shared.length > 0;
}
function isTrash(doc) {
    return doc.folder && doc.folder.indexOf("Trash") == 0;
}
//before start
var countBefore = { folders: {}, files: {} };
countBefore.folders.owner = db[COLLECTION_NAME].count({ file: { $exists: false }, "folder": { $not: /Trash.*/ }, 'shared.0': { $exists: false } })
countBefore.folders.shared = db[COLLECTION_NAME].count({ file: { $exists: false }, "folder": { $not: /Trash.*/ }, 'shared.0': { $exists: true } })
countBefore.folders.deleted = db[COLLECTION_NAME].count({ file: { $exists: false }, "folder": { $regex: /Trash.*/ } })
countBefore.files.owner = db[COLLECTION_NAME].count({ file: { $exists: true }, "folder": { $not: /Trash.*/ }, 'shared.0': { $exists: false } })
countBefore.files.shared = db[COLLECTION_NAME].count({ file: { $exists: true }, "folder": { $not: /Trash.*/ }, 'shared.0': { $exists: true } })
countBefore.files.deleted = db[COLLECTION_NAME].count({ file: { $exists: true }, "folder": { $regex: /Trash.*/ } })
//count ignore
var countIgnore = { folders: { deleted: 0, shared: 0, owner: 0 }, files: { deleted: 0, shared: 0, owner: 0 } };
//
db[COLLECTION_NAME].find({}).sort({ file: 1, folder: 1 }).forEach(function (doc) {
    var notSaved = !doc.eType;
    //eType
    //print("start;" + doc._id + ";" + doc.name + ";" + isFile(doc))
    if (isFile(doc)) {
        doc.eType = "file";
        doc.oldName = doc.name;
    } else {
        doc.eType = "folder";
        pushFolder(doc)
        doc.oldName = doc.name;
        doc.name = getLastPath(doc);
    }
    //shared
    if (hasShared(doc)) {
        replaceShared(doc);
        doc.inheritedShares = doc.shared;
        doc.isShared = true;
    } else {
        doc.shared = [];
        doc.inheritedShares = [];
        doc.isShared = false;
    }
    //deleted
    if (isTrash(doc)) {
        doc.deleted = true;
        doc.trasher = doc.owner;
    } else {
        doc.deleted = false;
    }
    var parent = null;
    //tree (not trash)
    if (isFile(doc)) {//file
        parent = findParentFor(doc, doc.folder);
    } else if (doc.oldName.indexOf("_")) {//subfolder
        parent = findParentFor(doc, getAllButLastPath(doc));
    } else {
        //folder root
    }
    //eParentOld
    if (parent) {
        if (doc.deleted && !parent.deleted) {
            doc.eParentOld = parent._id;
            doc.eParent = null;
        } else {
            doc.eParent = parent._id;
        }
    } else {
        doc.eParent = null
    }
    //set
    var update = { '$set': {} }
    update["$set"]["eType"] = doc.eType;
    update["$set"]["shared"] = doc.shared;
    update["$set"]["inheritedShares"] = doc.inheritedShares;
    update["$set"]["isShared"] = doc.isShared;
    update["$set"]["eParent"] = doc.eParent;
    update["$set"]["deleted"] = doc.deleted;
    if (doc.trasher)
        update["$set"]["trasher"] = doc.trasher;
    if (doc.eParentOld)
        update["$set"]["eParentOld"] = doc.eParentOld;
    //
    if (notSaved) {
        db[COLLECTION_NAME].update({ _id: doc._id }, update)
        //print("end;with save")
    } else {
        //print("end;without save")
        var temp = doc.eType == "file" ? countIgnore.files : countIgnore.folders
        if (doc.deleted) {
            temp.deleted = (temp.deleted) + 1;
        } else if (doc.isShared) {
            temp.shared = (temp.shared) + 1;
        } else {
            temp.owner = (temp.owner) + 1;
        }
    }
})
var countAfter = { folders: {}, files: {} };
countAfter.folders.owner = db[COLLECTION_NAME].count({ eType: "folder", "deleted": false, 'isShared': false })
countAfter.folders.shared = db[COLLECTION_NAME].count({ eType: "folder", "deleted": false, 'isShared': true })
countAfter.folders.deleted = db[COLLECTION_NAME].count({ eType: "folder", "deleted": true })
countAfter.files.owner = db[COLLECTION_NAME].count({ eType: "file", "deleted": false, 'isShared': false })
countAfter.files.shared = db[COLLECTION_NAME].count({ eType: "file", "deleted": false, 'isShared': true })
countAfter.files.deleted = db[COLLECTION_NAME].count({ eType: "file", "deleted": true })

print("report;owner;shared;deleted")
print("folder-before;" + countBefore.folders.owner + ";" + countBefore.folders.shared + ";" + countBefore.folders.deleted)
print("folder-after;" + countAfter.folders.owner + ";" + countAfter.folders.shared + ";" + countAfter.folders.deleted)
print("file-before;" + countBefore.files.owner + ";" + countBefore.files.shared + ";" + countBefore.files.deleted)
print("file-after;" + countAfter.files.owner + ";" + countAfter.files.shared + ";" + countAfter.files.deleted)
print("folder-ignored;" + countIgnore.folders.owner + ";" + countIgnore.folders.shared + ";" + countIgnore.folders.deleted)
print("file-ignored;" + countIgnore.files.owner + ";" + countIgnore.files.shared + ";" + countIgnore.files.deleted)

