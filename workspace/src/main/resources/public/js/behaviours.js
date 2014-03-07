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
}

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
			resource.myRights.share = true;
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
	}
});