var workspaceBehaviours = {
	resources: {
		comment: {
			right: 'org-entcore-workspace-service-WorkspaceService|commentDocument'
		},
		copy: {
			right: 'org-entcore-workspace-service-WorkspaceService|copyDocuments'
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
		}
	},
	root: {
		documents: {
			right: '',
			behaviours: {
				create: {
					right: ''
				}
			}
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
		resource.myRights = {};
		for(var behaviour in workspaceBehaviours.resources){
			if(model.me.hasRight(resource, workspaceBehaviours.resources[behaviour].right) || model.me.userId === resource.owner){
				resource.myRights[behaviour] = workspaceBehaviours.resources[behaviour].right;
			}
		}

		if(model.me.userId === resource.owner){
			resource.myRights.share = true;
		}

		return resource;
	},
	workflow: function(){
		return workspaceBehaviours.root;
	},
	resourceRights: function(){
		return ['comment', 'copy', 'move', 'moveTrash']
	}
});