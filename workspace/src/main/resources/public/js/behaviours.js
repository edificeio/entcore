//Copyright. Tous droits réservés. WebServices pour l’Education.
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
	}
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
	search: function(searchText, callback){
		http().get('/workspace/documents', { filter: 'owner' }).done(function(documents){
			callback(
				_.map(
					_.filter(documents, function(doc) {
						return lang.removeAccents(doc.name.toLowerCase()).indexOf(lang.removeAccents(searchText).toLowerCase()) !== -1 || doc._id === searchText;
					}),
					function(doc){
						if(doc.metadata['content-type'].indexOf('image') !== -1){
							doc.icon = '/workspace/document/' + doc._id + '?thumbnail=120x120';
						}
						else{
							doc.icon = '/img/icons/unknown-large.png';
						}
						return {
							title: doc.name,
							ownerName: doc.ownerName,
							owner: doc.owner,
							icon: doc.icon,
							path: '/workspace/document/' + doc._id,
							id: doc._id
						};
					}
				)
			);
		})
	}
});