import { Behaviours, http, idiom as lang, _, ui } from 'entcore';

console.log('directory behaviours loaded');

Behaviours.register('directory', {
	rights:{
		workflow: {
			externalNotifications: "org.entcore.timeline.controllers.TimelineController|mixinConfig",
			historyView: "org.entcore.timeline.controllers.TimelineController|historyView",
			showMoodMotto: "org.entcore.directory.controllers.UserBookController|userBookMottoMood",
			switchTheme: "org.entcore.directory.controllers.UserBookController|userBookSwitchTheme",
            generateMergeKey: "org.entcore.directory.controllers.UserController|generateMergeKey",
			mergeByKey: "org.entcore.directory.controllers.UserController|mergeByKey",
			allowSharebookmarks: "org.entcore.directory.controllers.ShareBookmarkController|allowSharebookmarks",
			allowLoginUpdate: "org.entcore.directory.controllers.UserController|allowLoginUpdate"
		}
	},
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
				viewUserInfos: function(userId){
				    window.location.href = '/userbook/annuaire#/' + userId;
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
					var that = this
					http().get('/userbook/structure/' + this.search.structure.id).done(function(structure){
						this.search.groups = structure.profileGroups.concat(structure.manualGroups);
						_.map(this.search.groups, function(group){ group.translatedName = that.groupTranslation(group.name) })
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
					return ui.profileColors.match(type);
				},
				groupTranslation: function(groupName){
					var splittedName = groupName.split('-')
					return splittedName.length > 1 ?
						lang.translate(groupName.substring(0, groupName.lastIndexOf('-'))) + '-' + lang.translate(groupName.split('-')[splittedName.length - 1]) :
						groupName
				}
			}
		}
	}
});
