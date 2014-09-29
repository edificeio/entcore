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
var workspaceBehaviours = {
	resources: {
		comment: {
			right: 'org-entcore-workspace-service-WorkspaceService|commentDocument'
		},
		copy: {
			right: 'org-entcore-workspace-service-WorkspaceService|copyDocuments',
			workflow: 'org.entcore.workspace.service.WorkspaceService|copyRackDocument'
		},
		move: {
			right: 'org-entcore-workspace-service-WorkspaceService|moveDocument'
		},
		moveTrash: {
			right: 'org-entcore-workspace-service-WorkspaceService|moveTrash'
		},
		read: {
			right: 'org-entcore-workspace-service-WorkspaceService|getDocument'
		},
		edit: {
			right: 'org-entcore-workspace-service-WorkspaceService|updateDocument'
		},
		share: {
			right: 'manager',
			workflow: 'org.entcore.workspace.service.WorkspaceService|shareJson'
		}
	},
	workflow: {
		documents: {
			list: 'org.entcore.workspace.service.WorkspaceService|listDocuments',
			create: 'org.entcore.workspace.service.WorkspaceService|addDocument',
			copy: '',
			share: ''
		},
		rack: {
			right: '',
			behaviours: {
				send: {
					right: ''
				}
			}
		}
	},
	viewRights: ["org-entcore-workspace-service-WorkspaceService|copyDocuments", "org-entcore-workspace-service-WorkspaceService|getDocument"]
};

Behaviours.register('workspace', {
	resource: function(resource){
		if(!resource.myRights){
			resource.myRights = {};
		}

		for(var behaviour in workspaceBehaviours.resources){
			if(model.me.hasRight(resource, workspaceBehaviours.resources[behaviour]) || model.me.userId === resource.owner){
				if(resource.myRights[behaviour] !== undefined){
					resource.myRights[behaviour] = resource.myRights[behaviour] && workspaceBehaviours.resources[behaviour];
				}
				else{
					resource.myRights[behaviour] = workspaceBehaviours.resources[behaviour];
				}
			}
		}

		if(model.me.userId === resource.owner){
			resource.myRights.share = workspaceBehaviours.resources[behaviour];
		}

		return resource;
	},
	workflow: function(){
		var workflow = { documents: {}, rack: {}};
		var documentsWorkflow = workspaceBehaviours.workflow.documents;
		for(var prop in documentsWorkflow){
			if(model.me.hasWorkflow(documentsWorkflow[prop])){
				workflow.documents[prop] = true;
			}
		}

		return workflow;
	},
	resourceRights: function(){
		return ['comment', 'copy', 'move', 'moveTrash']
	},
	loadResources: function(callback){
		http().get('/workspace/documents').done(function(documents){
			this.resources = _.map(documents, function(doc){
				if(doc.metadata['content-type'].indexOf('image') !== -1){
					doc.icon = '/workspace/document/' + doc._id + '?thumbnail=150x150';
				}
				else{
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
					_id: doc._id
				};
			});
			if(typeof callback === 'function'){
				callback(this.resources);
			}
		}.bind(this));
	},
	create: function(file, callback){
		file.loading = true;
		var splitName = file.file[0].name.split('.');
		var formData = new FormData();
		formData.append('file', file.file[0], file.title + '.' + splitName[splitName.length - 1]);
		http().postFile('/workspace/document', formData).done(function(data){
			file.loading = false;
			this.loadResources(callback);
		}.bind(this));
	},
	sniplets: {
		documents: {
			title: 'Documents',
			description: 'Il vous permet d\'ajouter des documents que vos visiteurs pourront télécharger',
			controller: {
				initSource: function(){
					this.setSnipletSource({
						documents: []
					});
				},
				init: function(){
					http().get('/workspace/documents', { filter: 'owner' }).done(function(data){
						this.documents = data;
						this.$apply();
					}.bind(this))
				},
				addDocument: function(document){
					this.source.documents.push(document);
					Behaviours.copyRights(this.snipletResource, document, workspaceBehaviours.viewRights, 'workspace');
					this.display.selectSnipletDocument = false;
				},
				copyRights: function(snipletResource, source){
					source.documents.forEach(function(document){
						Behaviours.copyRights(snipletResource, document, workspaceBehaviours.viewRights, 'workspace');
					});
				},
				documentIcon: function(doc){
					if(doc.metadata['content-type'].indexOf('image') !== -1){
						return '/workspace/document/' + doc._id + '?thumbnail=150x150';
					}
					else{
						return '/img/icons/unknown-large.png';
					}
				}
			}
		}
	}
});