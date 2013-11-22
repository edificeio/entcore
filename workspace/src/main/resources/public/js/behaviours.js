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
			right: 'edu-one-core-workspace-service-WorkspaceService|commentDocument',
			apply: function(){

			}
		},
		copy: {
			right: 'edu-one-core-workspace-service-WorkspaceService|moveDocument'
		},
		move: {
			right: 'edu-one-core-workspace-service-WorkspaceService|moveDocument'
		},
		moveTrash: {
			right: 'edu-one-core-workspace-service-WorkspaceService|moveTrash'
		}
	},
	root: {
		create: function(){

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
	root: function(){
		return behaviours.root;
	}
});