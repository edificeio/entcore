// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

import { Behaviours, http, workspace, notify, Document } from 'entcore';
import { _ } from 'entcore';
/**
 * JS apps dont have es6 modules
 */
function getWorkspaceService() {
	let service = workspace.v2.service;
	if (!service) {
		service = (window as any).entcore.workspace.v2.service;
	}
	return service;
}
function getElementClass() {
	let models = workspace.v2.models;
	if (!models) {
		models = (window as any).entcore.workspace.v2.models;
	}
	return models.Element;
}
function getDocumentClazz() {
	let clazz =Document;
	if (!clazz || !clazz.prototype.saveChanges) {
		clazz = (window as any).entcore.workspace.v2.models;
	}
	return clazz;
}
function getNotify() {
	let temp = notify;
	if (!temp) {
		temp = (<any>window).entcore.notify
	}
	return temp;
}
console.log('workspace behaviours loaded');

Behaviours.register('workspace', {
	rights: {
		resource: {
			comment: {
				right: 'org-entcore-workspace-controllers-WorkspaceController|commentDocument'
			},
			contrib: {
				right: "org-entcore-workspace-controllers-WorkspaceController|updateDocument"
			},
			read: {
				right: 'org-entcore-workspace-controllers-WorkspaceController|getDocument'
			},
			manager: {
				right: 'org-entcore-workspace-controllers-WorkspaceController|shareJson'
			}
		},
		workflow: {
			list: 'org.entcore.workspace.controllers.WorkspaceController|listDocuments',
			create: 'org.entcore.workspace.controllers.WorkspaceController|addDocument',
			copy: '',
			share: '',
			renameDocument: 'org.entcore.workspace.controllers.WorkspaceController|renameDocument',
			renameFolder: 'org.entcore.workspace.controllers.WorkspaceController|renameFolder'
		},
		viewRights: ["org-entcore-workspace-controllers-WorkspaceController|copyDocuments", "org-entcore-workspace-controllers-WorkspaceController|getDocument"]
	},
	loadResources: async function (callback) {
		const docs = await getWorkspaceService().fetchDocuments({ filter: "all", hierarchical: true })
		this.resources = docs.filter(d => !d.deleted).map((doc) => {
			if (doc.metadata['content-type'].indexOf('image') !== -1) {
				doc.icon = '/workspace/document/' + doc._id + '?thumbnail=150x150';
			}
			else {
				doc.icon = '/img/icons/unknown-large.png';
			}
			return {
				title: doc.name,
				owner: {
					name: doc.ownerName,
					userId: doc.owner
				},
				icon: doc.icon,
				path: '/workspace/document/' + doc._id,
				_id: doc._id,
				metadata: doc.metadata,
				protected: doc.protected
			};
		});
		if (typeof callback === 'function') {
			callback(this.resources);
		}

	},
	create: async function (file, callback) {
		console.log('creating file');
		const splitName = file.file[0].name.split('.');
		const ext = splitName[splitName.length - 1];
		if (file.title !== file.file[0].name) {
			file.title += ('.' + ext);
		}
		let clazz = getDocumentClazz()
		let doc = new clazz
		doc.name = file.title;
		file.loading = true;
		try {
			const data = await getWorkspaceService().createDocument(file.file[0], doc, null, { visibility: "protected", application: "media-library" })
			file.loading = false;
			this.loadResources(function (resources) {
				file.title = splitName[0];
				callback(resources, data);
			});
		} catch (e) {
			file.loading = false;
			var error = JSON.parse(e.responseText || e);
			//can't use notify.error, notify is undefined in not TS app like support
			getNotify().error(error.error);
		}
	},
	duplicate: async function (file, visibility, callback) {
		console.log(file);
		const res = await getWorkspaceService().copyDocumentWithVisibility(file, { visibility, application: "media-library" })
		callback(res);
	},
	publicDuplicate: function (file, callback) {
		this.duplicate(file, 'public', callback);
	},
	protectedDuplicate: function (file, callback) {
		this.duplicate(file, 'protected', callback);

	},
	sniplets: {
		documents: {
			public: true,
			title: 'workspace.sniplet.documents.title',
			description: 'workspace.sniplet.documents.description',
			controller: {
				initSource: function () {
					this.setSnipletSource({
						documents: []
					});
				},
				init: function () {
					this.visibility = 'protected';
					if (this.snipletResource.visibility === 'PUBLIC') {
						this.visibility = 'public';
					}
					this.displaySniplet = {};
					this.create = {
						document: {},
						folder: {
							documents: []
						}
					};
					this.source.title = 'Documents';
					if (!this.source.documents) {
						this.source.documents = [];
					}
					this.cursor = {
						currentFolder: this.source,
						parentFolders: [],
						selection: []
					};
					this.cursor.currentFolder.documents = _.map(this.cursor.currentFolder.documents, function (item) {
						delete item.selected;
						return item;
					});
					this.folder = this.source;
				},
				isFolder: function (document) {
					return document.metadata === undefined;
				},
				addFolder: function () {
					console.log('adding new folder in documents');
					this.cursor.currentFolder.documents.push(this.create.folder);
					this.cursor.parentFolders.push(this.cursor.currentFolder);
					this.cursor.currentFolder = this.create.folder;
					this.create.folder = {
						title: '',
						documents: []
					};
					if (typeof this.snipletResource.save === 'function') {
						this.snipletResource.save();
					}
				},
				updateSelection: function () {
					this.cursor.selection = _.where(this.cursor.currentFolder.documents, { selected: true });
				},
				openFolder: function (folder) {
					if (this.cursor.parentFolders.indexOf(folder) !== -1) {
						var folderIndex = this.cursor.parentFolders.indexOf(folder);
						this.cursor.parentFolders.splice(folderIndex, this.cursor.parentFolders.length - folderIndex);
					}
					else {
						this.cursor.parentFolders.push(this.cursor.currentFolder);
					}

					this.cursor.currentFolder = folder;
					this.cursor.currentFolder.documents = _.map(this.cursor.currentFolder.documents, function (item) {
						delete item.selected;
						return item;
					});
					this.cursor.selection = [];
				},
				openDocument: function (document) {
					if (!document.metadata) {
						this.openFolder(document);
					}
					else {
						if (this.visibility === 'public') {
							window.location.href = '/workspace/pub/document/' + document._id;
						}
						else {
							window.location.href = '/workspace/document/' + document._id;
						}
					}
				},
				addDocument: function (document) {
					console.log('adding ' + JSON.stringify(document) + ' in documents');
					Behaviours.applicationsBehaviours.workspace.loadResources(function (resources) {
						document = _.findWhere(resources, { _id: document._id });

						this.cursor.currentFolder.documents.push(document);
						if (typeof this.snipletResource.save === 'function') {
							this.snipletResource.save();
						}
						this.displaySniplet.pickFile = false;

						this.$apply();
					}.bind(this));
				},
				drag: function (item, $originalEvent) {
					$originalEvent.dataTransfer.setData('Text', JSON.stringify(item));
				},
				dropTo: function (targetItem, $originalEvent) {
					if (!targetItem || targetItem.path) {
						return;
					}
					var originalItem = JSON.parse($originalEvent.dataTransfer.getData('Text'));
					var foundItem = _.find(this.cursor.currentFolder.documents, function (document) {
						return (document._id === originalItem._id && originalItem.metadata) || (document._id === undefined && originalItem.title === document.title);
					});

					this.displaySniplet.targetFolder = targetItem;
					this.clearSelection();
					foundItem.selected = true;
					this.updateSelection();
					this.moveDocuments();
					this.$apply();
				},
				clearSelection: function () {
					this.cursor.selection.forEach(function (doc) {
						doc.selected = false;
					});
					this.cursor.selection = [];
				},
				removeDocuments: function (document) {
					this.cursor.currentFolder.documents = _.reject(this.cursor.currentFolder.documents, function (doc) {
						return doc.selected;
					});
					this.cursor.selection = [];
					if (typeof this.snipletResource.save === 'function') {
						this.snipletResource.save();
					}
				},
				moveDocuments: function () {
					this.cursor.currentFolder.documents = _.reject(this.cursor.currentFolder.documents, function (doc) {
						return doc.selected;
					});
					this.displaySniplet.targetFolder.documents = this.displaySniplet.targetFolder.documents.concat(this.cursor.selection);
					this.clearSelection();
					if (typeof this.snipletResource.save === 'function') {
						this.snipletResource.save();
					}
				},
				getReferencedResources: function (source) {
					return _.map(_.filter(source.documents, function (doc) { return doc.icon }), function (doc) {
						var spl = doc.icon.split('/');
						return spl[spl.length - 1];
					});
				},
				documentIcon: function (doc) {
					if (doc.metadata['content-type'].indexOf('image') !== -1) {
						if (this.snipletResource.visibility === 'PUBLIC') {
							return '/workspace/pub/document/' + doc._id + '?thumbnail=150x150';
						}
						else {
							return '/workspace/document/' + doc._id + '?thumbnail=150x150';
						}

					}
					else {
						return '/img/icons/unknown-large.png';
					}
				}
			}
		},
		carousel: {
			public: true,
			title: 'workspace.sniplet.carousel.title',
			description: 'workspace.sniplet.carousel.description',
			controller: {
				initSource: function () {
					this.carousel = {
						documents: []
					};
				},
				init: function () {
					this.visibility = 'protected';
					if (this.snipletResource.visibility === 'PUBLIC') {
						this.visibility = 'public';
					}
					http().get('/workspace/documents', { filter: 'owner' }).done(function (data) {
						this.documents = data;
						this.$apply();
					}.bind(this))
				},
				setSource: function () {
					this.setSnipletSource(this.carousel);
				},
				addDocument: function (document) {
					console.log('adding ' + document + ' in carousel');
					this.display.isLoading = true;
					this.source.documents.push({
						icon: document,
						link: document
					});
					if (typeof this.snipletResource.save === 'function') {
						this.snipletResource.save();
					}
				},
				removeDocument: function (document) {
					this.source.documents = _.reject(this.source.documents, function (doc) {
						return doc._id === document._id;
					});
					if (typeof this.snipletResource.save === 'function') {
						this.snipletResource.save();
					}
				},
				getReferencedResources: function (source) {
					return _.map(source.documents, function (doc) {
						var spl = doc.icon.split('/');
						return spl[spl.length - 1];
					});
				}
			}
		}
	}
});
