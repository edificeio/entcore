import { http } from './http';
import { Behaviours } from './behaviours';
import { moment } from './libs/moment/moment';
import { model, Model, Collection } from './modelDefinitions';
import { _ } from './libs/underscore/underscore';
import { notify } from './notify';
import { idiom as lang } from './idiom';

class Quota extends Model {
    max: number;
    used: number;
    unit: string;

    constructor() {
        super();
        this.max = 1;
        this.used = 0;
        this.unit = 'Mo';
    }

    appropriateDataUnit(bytes: number) {
        var order = 0
        var orders = {
            0: lang.translate("byte"),
            1: "Ko",
            2: "Mo",
            3: "Go",
            4: "To"
        }
        var finalNb = bytes
        while (finalNb >= 1024 && order < 4) {
            finalNb = finalNb / 1024
            order++
        }
        return {
            nb: finalNb,
            order: orders[order]
        }
    }

    refresh () {
        http().get('/workspace/quota/user/' + model.me.userId).done((data) => {
            //to mo
            data.quota = data.quota / (1024 * 1024);
            data.storage = data.storage / (1024 * 1024);

            if (data.quota > 2000) {
                data.quota = Math.round((data.quota / 1024) * 10) / 10;
                data.storage = Math.round((data.storage / 1024) * 10) / 10;
                this.unit = 'Go';
            }
            else {
                data.quota = Math.round(data.quota);
                data.storage = Math.round(data.storage);
            }

            this.max = data.quota;
            this.used = data.storage;
            this.trigger('change');
        });
    }
};

export let quota = new Quota();

export class Revision extends Model{
	constructor(data){
		super(data);
	}
}

export class Document extends Model {
    title: string;
    _id: string;
    created: any;
    metadata: {
        'content-type': string,
		role: string,
		extension: string
    };
	version: number;
	link: string;
	icon: string;
	owner: {
		userId: string
	};
	revisions: Collection<Revision>;


    constructor(data) {
		super(data);

        if (data.metadata) {
            var dotSplit = data.metadata.filename.split('.');
            if (dotSplit.length > 1) {
                dotSplit.length = dotSplit.length - 1;
            }
            this.title = dotSplit.join('.');
			this.metadata.role = this.role();
        }

        if (data.created) {
            this.created = moment(data.created.split('.')[0]);
        }
		else if(data.sent){
			this.created = moment(data.sent.split('.')[0]);
        }
        else {
            this.created = moment();
        }

		this.owner = { userId: data.owner };

		this.version = parseInt(Math.random() * 100);
		this.link = '/workspace/document/' + this._id;
		if(this.metadata.role === 'img'){
			this.icon = this.link;
		}
		this.collection(Revision);
    }

	refreshHistory(hook?: () => void){
		http().get("document/" + this._id + "/revisions").done((revisions) => {
			this.revisions.load(revisions);
			if(typeof hook === 'function'){
				hook()
			}
		})
	}

    upload(file: File | Blob, requestName: string, callback: (data: any) => void, visibility?: 'public' | 'protected') {
        if (!visibility) {
            visibility = 'protected';
        }
        var formData = new FormData();
        formData.append('file', file, file.name);
        http().postFile('/workspace/document?' + visibility + '=true&application=media-library&quality=0.7&' + workspace.thumbnails, formData, { requestName: requestName }).done(function (data) {
            if (typeof callback === 'function') {
                callback(data);
            }
        }).e400(function (e) {
            var error = JSON.parse(e.responseText);
            notify.error(error.error);
        });
    }

    role() {
        return Document.role(this.metadata['content-type']);
    }

    protectedDuplicate(callback?: (document: Document) => void) {
        Behaviours.applicationsBehaviours.workspace.protectedDuplicate(this, function (data) {
            if (typeof callback === 'function') {
                callback(new workspace.Document(data))
            }
        });
    }

    publicDuplicate(callback?: (document: Document) => void) {
        Behaviours.applicationsBehaviours.workspace.publicDuplicate(this, function (data) {
            if (typeof callback === 'function') {
                callback(new workspace.Document(data))
            }
        });
    }

    static role(fileType) {
        if (!fileType)
            return 'unknown'

        var types = {
            'doc': function (type) {
                return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
            },
            'xls': function (type) {
                return (type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1) || (type.indexOf('ms-excel') !== -1);
            },
            'img': function (type) {
                return type.indexOf('image') !== -1;
            },
            'pdf': function (type) {
                return type.indexOf('pdf') !== -1 || type === 'application/x-download';
            },
            'ppt': function (type) {
                return (type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1) || type.indexOf('powerpoint') !== -1;
            },
            'video': function (type) {
                return type.indexOf('video') !== -1;
            },
            'audio': function (type) {
                return type.indexOf('audio') !== -1;
            },
            'zip': function (type) {
                return type.indexOf('zip') !== -1 ||
                    type.indexOf('rar') !== -1 ||
                    type.indexOf('tar') !== -1 ||
                    type.indexOf('7z') !== -1;
            }
        };

        for (let type in types) {
            if (types[type](fileType)){
                return type;
            }
        }

        return 'unknown';
    }

    trash(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().put('/workspace/document/trash/' + this._id).done(() => {
                resolve();
            });
        });
    }
}

export let workspace = {
	thumbnails: "thumbnail=120x120&thumbnail=150x150&thumbnail=100x100&thumbnail=290x290&thumbnail=48x48&thumbnail=82x82&thumbnail=381x381&thumbnail=1600x0",
	Document: Document,
	Folder: function(data){
		this.updateData(data);

		this.collection(workspace.Folder, {
			sync: function(){
				this.load(_.filter(model.mediaLibrary.myDocuments.folders.list, function(folder){
					return folder.folder.indexOf(data.folder + '_') !== -1;
				}));
			}
		});

		this.collection(workspace.Document,  {
			sync: function(){
				http().get('/workspace/documents/' + data.folder, { filter: 'owner', hierarchical: true }).done(function(documents){
					this.load(documents);
				}.bind(this));
			}
		});

		this.closeFolder = function(){
			this.folders.all = [];
		};

		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	MyDocuments: function(){
		this.collection(workspace.Folder, {
			sync: function(){
				if(model.me.workflow.workspace.create){
					http().get('/workspace/folders/list', { filter: 'owner' }).done(function(data){
						this.list = data;
						this.load(_.filter(data, function(folder){
							return folder.folder.indexOf('_') === -1;
						}))
					}.bind(this));
				}
			},
			list: []
		});

		this.collection(workspace.Document,  {
			sync: function(){
				http().get('/workspace/documents', { filter: 'owner', hierarchical: true }).done(function(documents){
					this.load(documents);
				}.bind(this))
			}
		});

		this.on('folders.sync, documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	SharedDocuments: function(){
		this.collection(workspace.Document,  {
			sync: function(){
				if(model.me.workflow.workspace.list){
					http().get('/workspace/documents', { filter: 'shared' }).done(function(documents){
						this.load(documents);
					}.bind(this));
				}
			}
		});
		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	PublicDocuments: function(){
		this.collection(workspace.Document, {
			sync: function(){
				http().get('/workspace/documents', { filter: 'public', application: 'media-library' }).done(function(documents){
					this.load(_.filter(documents, function(doc){
						return doc.folder !== 'Trash';
					}));
				}.bind(this))
			}
		});
		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	},
	AppDocuments: function(){
		this.collection(workspace.Document, {
			sync: function(){
				http().get('/workspace/documents', { filter: 'protected' }).done(function(documents){
					this.load(_.filter(documents, function(doc){
						return doc.folder !== 'Trash';
					}));
				}.bind(this))
			}
		});
		this.on('documents.sync', function(){
			this.trigger('sync');
		}.bind(this));
	}
};

if (!(window as any).entcore) {
    (window as any).entcore = {};
}
(window as any).entcore.workspace = workspace;
(window as any).entcore.quota = quota;
(window as any).workspace = workspace;