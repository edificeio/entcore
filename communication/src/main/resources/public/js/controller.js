function CommunicationAdminController($scope, $http){

	$scope.themes = [
		{
			name: "pink",
			path: "default"
		},
		{
			name: "orange",
			path: "orange"
		},
		{
			name: "blue",
			path: "blue"
		},
		{
			name: "purple",
			path: "purple"
		},
		{
			name: "red",
			path: "red"
		},
		{
			name: "green",
			path: "green"
		},
		{
			name: "grey",
			path: "grey"
		}
	]
	$scope.setTheme = function(theme){
		ui.setStyle('/public/admin/'+theme.path+'/')
		http().putJson('/userbook/preference/admin', {
			name: theme.name,
			path: theme.path
		})
	}

	$scope.lang = lang
	$scope.structures = model.structures
	$http.get('rules').success(function(rules){ $scope.defaultRules = _.map(rules, function(val, key){ val.name = key; return val }) })

	//Cannot be used directly in the html file due to brackets in notation.
	$scope.getRelativeStudentProp = function(group){ return group['Relative-Student'] }

	//TODO : palliatif - à enlever
	model.scope = $scope

	$scope.viewStructure = function(structure){
		$scope.$parent.structure = structure
		structure.groups.sync()
	}

	$scope.filterTopStructures = function(structure){
		return !structure.parents
	}

	$scope.selectOnly = function(structure, structureList){
		_.forEach(structure.children, function(s){ s.selected = false })
		_.forEach(structureList, function(s){ s.selected = s.id === structure.id ? true : false })
	}

	$scope.viewGroup = function(group){
		group.getCommunication()
	}

	$scope.filterGroupsFunction = function(input){
		return function(group){
			return !input ? true : lang.removeAccents($scope.groupTranslation(group.name).toLowerCase()).indexOf(lang.removeAccents(input.toLowerCase())) >= 0
		}
	}

	$scope.accordionClick = function(sectionNb){
		$scope.section = $scope.section === sectionNb ? 0 : sectionNb
	}

	$scope.toggleGroupCommunication = function(group, targetGroup) {
		if(!_.findWhere(group.communiqueWith, { id: targetGroup.id})){
			group.addGroupLink(targetGroup.id)
		} else {
			group.removeGroupLink(targetGroup.id)
		}
	}

	$scope.groupTranslation = function(groupName){
		var splittedName = groupName.split('-')
		return splittedName.length > 1 ?
			lang.translate(groupName.substring(0, groupName.lastIndexOf('-'))) + '-' + lang.translate(groupName.split('-')[splittedName.length - 1]) :
			groupName
	}

	$scope.groupStyling = function(group, listedGroup){
		var otherGroup = _.findWhere(group.communiqueWith, { id: listedGroup.id })
		var color = !otherGroup ? "gray" : otherGroup.communiqueWith && otherGroup.communiqueWith.indexOf(group.id) >= 0 ? "darkgreen" : "crimson"
		return { 'border-color' : color }
	}
	$scope.commStyling = function(group1, group2){
		var otherGroupIndex =  group1.communiqueWith ? group1.communiqueWith.indexOf(group2.name) : -1
		var color = otherGroupIndex < 0 ? "inherit" : group2.communiqueWith && group2.communiqueWith.indexOf(group1.name) >= 0 ? "darkgreen" : "crimson"
		return { 'background-color' : color }
	}

	$scope.filterAllOtherGroups = function(groupList, group){
		return _.reject(groupList, function(g){ return group.id === g.id })
	}

	$scope.modifyInnerGroupRules = function(group) {
		group.addLinksWithUsers(group.communiqueUsers)
	}

	$scope.deleteInnerGroupRules = function(group) {
		group.removeLinksWithUsers("BOTH")
		group.communiqueUsers = ""
	}

	$scope.modifyRelativeGroupRules = function(group) {
		group.addLinkBetweenRelativeAndStudent(group.relativeCommuniqueStudent)
	}

	$scope.deleteRelativeGroupRules = function(group) {
		group.removeLinkBetweenRelativeAndStudent("BOTH")
		group.relativeCommuniqueStudent = ""
	}

}
