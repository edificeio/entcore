function hasRight(resource, name){
	var currentSharedRights = _.filter(resource.shared, function(sharedRight){
		return Model.me.profilGroupsIds.indexOf(sharedRight.groupId) !== -1
			|| sharedRight.userId === Model.me.userId;
	});

	return _.find(currentSharedRights, function(right){
		return right[name];
	}) !== undefined;
}

var behaviours = {
	resources: {
		comment: {
			right: 'org-entcore-workspace-service-WorkspaceService|commentDocument',
			apply: function(){

			}
		},
		copy: {
			right: 'org-entcore-workspace-service-WorkspaceService|moveDocument'
		},
		move: {
			right: 'org-entcore-workspace-service-WorkspaceService|moveDocument'
		},
		moveTrash: {
			right: 'org-entcore-workspace-service-WorkspaceService|moveTrash'
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
		for(var behaviour in behaviours.resources){
			if(hasRight(resource, behaviours.resources[behaviour].right)){
				resource[behaviour] = behaviours.resources[behaviour].right;
			}
		}

		return resource;
	},
	workflow: function(){
		return behaviours.root;
	}
});