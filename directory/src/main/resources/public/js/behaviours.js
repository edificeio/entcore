console.log('directory behaviours loaded');

Behaviours.register('directory', {
	sniplets: {
		facebook: {
			title: 'sniplet.facebook.title',
			description: 'sniplet.facebook.desc',
			controller: {
				initSource: function(){
					this.source = {
						groups: []
					};
					this.search = {
						text: '',
						groups: [],
						structures: [],
						structure: null
					};

					http().get('/userbook/structures').done(function(structures){
						this.search.structures = structures;
						this.$apply('search');
					}.bind(this));
				},
				removeGroup: function(index, group){
					this.source.groups.splice(index, 1);
					this.search.groups.push(group);
				},
				addGroup: function(group){
					this.source.groups.push(group);
					var index = this.search.groups.indexOf(group);
					this.search.groups.splice(index, 1);
				},
				loadGroups: function(){
					http().get('/userbook/structure/' + this.search.structure.id).done(function(structure){
						this.search.groups = structure.profileGroups.concat(structure.manualGroups);
						this.$apply('search');
					}.bind(this));
				},
				init: function(){
					this.source.groups.forEach(function(group){
						http().get('/userbook/visible/users/' + group.id).done(function(users){
							group.users = users;
							this.$apply('source');
						}.bind(this));
					}.bind(this))

				},
				applySource: function(){
					this.setSnipletSource(this.source);
				},
				colorFromType: function(type){
					var colorsMatch = { relative: 'cyan', teacher: 'green', student: 'orange', personnel: 'purple' };
					return colorsMatch[type.toLowerCase()];
				}
			}
		}
	}
});

