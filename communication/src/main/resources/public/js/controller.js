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
			return !input ? true : group.name.toLowerCase().indexOf(input.toLowerCase()) >= 0
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
