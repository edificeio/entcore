routes.define(function($routeProvider){
	$routeProvider
		.when('/', {
			action: 'default'
		})
		.when('/structureUser/:structureId/:userId', {
			action: 'viewStructureUser'
		})
		.when('/structure/:structureId', {
			action: 'viewStructure'
		})
        .when('/class/:structureId/:classId', {
            action: 'viewClass'
        })
		.when('/classUsers/:structureId/:classId', {
			action: 'viewClassUsers'
		})
		.otherwise({
			redirectTo: '/'
		})
})

function AdminDirectoryController($scope, $rootScope, $http, $route, template, model, date, route){

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

    route({
		default: function(){
			$scope.structures.sync(function(){
				$scope.$apply()
			})
		},
        viewStructureUser: function(params){
            var userId = params.userId
            var structureId = params.structureId

			template.open('body', 'admin-user-tab')
			$scope.currentLeaf = "userTab"
			$scope.showWhat = 'showStructureUser'

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.selectOnly($scope.structure)
				$scope.reloadStructureAndRetrieveUser({ id: userId })()
			})
        },
        viewStructure: function(params){
            var structureId = params.structureId

			template.open('body', 'admin-structure-tab')
			$scope.currentLeaf = "structureTab"
			$scope.showWhat = 'showCurrentStructure'

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.selectOnly($scope.structure)
			})
        },
        viewClass: function(params){
            var classId = params.classId
            var structureId = params.structureId

			template.open('body', 'admin-class-tab')
			$scope.currentLeaf = "classTab"

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.selectOnly($scope.structure)
	            $scope.structure.classes.sync(function(){
	                $scope.classSelected = $scope.structure.classes.findWhere({ id: classId})
	                $scope.$apply()
	            })
			})
        },
		viewClassUsers: function(params){
			var classId = params.classId
			var structureId = params.structureId

			template.open('body', 'admin-user-tab')
			$scope.currentLeaf = "userTab"
			$scope.showWhat = 'showStructureUser'

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.selectOnly($scope.structure)
				$scope.structure.loadStructure(
					null,
					function(){
						$scope.userFilters.showIsolated = false
						$scope.deselectAllClasses()
						$scope.structure.classes.forEach(function(classe){
							if(classe.id === classId){
								classe.selected = true
								$scope.structure.addClassUsers(classe, [$scope.refreshScope])
							}
						})
					}
				)
			})
		}
	})

	$scope.template = template
	template.open('userDetails', 'admin-user-details')
    $scope.structures = model.structures.structures
    $scope.lang = lang

	$scope.DEFAULT_QUOTA_UNIT = 1048576

	$scope.scrollOpts = {
		SCROLL_INCREMENT: 250,
		scrollLimit: 250,
		increment: function(){
			this.scrollLimit = this.scrollLimit + this.SCROLL_INCREMENT
		},
		reset: function(){
			this.scrollLimit = this.SCROLL_INCREMENT
		}
	}

    $scope.refreshScope = function(){ $scope.$apply() }

    $scope.formatLongDate = function(d){
        return d ? date.create(d).format('LLLL') : ""
    }

	$scope.formatDate = function(date){
		return date ? moment(date).format('DD/MM/YYYY') : "";
	}

	$scope.formatUserFunctions = function(user){
		return _.chain(user.functions).map(function(f){ return f[0]}).uniq().map(function(f){ return lang.translate(f) }).value().join(", ")
	}

	$scope.isAdminLocal = function(){
		return _.findWhere(model.me.functions, {code: "ADMIN_LOCAL"}) !== undefined
	}

	$scope.isCentralAdmin = function(){
		return _.findWhere(model.me.functions, {code: "SUPER_ADMIN"}) !== undefined
	}

	$scope.isSelf = function(user){
		return user.id === model.me.userId
	}

	//// TOP MENU
	$scope.filterLeafMenuItems = function(item, index){
		return typeof item.showCondition !== "function" || item.showCondition()
	}

	$scope.currentLeaf = ""
	$scope.leafMenu = [
		{
			name: "userTab",
			text: lang.translate("directory.userOps"),
			templateName: 'admin-user-tab',
			onClick: function(){
				delete $scope.targetUser
				$scope.scrollOpts.reset()
			},
			requestName : "user-requests"
		},
		{
			name: "structureTab",
			text: lang.translate("directory.structureOps"),
			templateName: 'admin-structure-tab',
			onClick: function(){ $scope.scrollOpts.reset() }
		},
		{
			name: "classTab",
			text: lang.translate("directory.classOps"),
			templateName: 'admin-class-tab',
			onClick: function(){ $scope.scrollOpts.reset() }
		},
		{
			name: "groupTab",
			text: lang.translate("directory.groupOps"),
			templateName: 'admin-group-tab',
			onClick: function(){ $scope.scrollOpts.reset() },
			requestName : "groups-request"
		},
		{
			name: "maintenanceTab",
			text: lang.translate("directory.feeding"),
			templateName: 'admin-maintenance-tab',
			onClick: function(){ $scope.scrollOpts.reset() }
		},
		{
			name: "isolatedTab",
			text: lang.translate("directory.isolatedUsers"),
			templateName: 'admin-isolated-tab',
			onClick: function(){
				$scope.scrollOpts.reset();
				$scope.refreshIsolated()
			},
			requestName : "isolated-request",
			showCondition: function(){ return $scope.isCentralAdmin() }
		},
		{
			name: "duplicatesTab",
			text: lang.translate("directory.admin.duplicatesManagement"),
			templateName: 'admin-duplicates-tab',
			onClick: function(){ $scope.scrollOpts.reset() },
			requestName : "duplicates-request"
		},
		{
			name: "crossSearchTab",
			text: lang.translate("directory.trasversal.search"),
			templateName: 'admin-crosssearch-tab',
			onClick: function(){
				delete $scope.targetUser
				$scope.scrollOpts.reset()
			},
			requestName : "cross-search-request"
		}
	]
	_.forEach($scope.leafMenu, function(leaf){
		var temp = leaf.onClick
		leaf.onClick = function(){
			$scope.currentLeaf = leaf.name
			temp()
		}
	})

	$scope.notCrossSearchTab = function(){
		return $scope.currentLeaf !== 'crossSearchTab'
	}

    //Given a data size in bytes, returns a more "user friendly" representation.
    $scope.getAppropriateDataUnit = function(bytes){
        var order = 0
        var orders = {
            0: lang.translate("directory.byte"),
            1: lang.translate("directory.kilobyte"),
            2: lang.translate("directory.megabyte"),
            3: lang.translate("directory.gigabyte"),
            4: lang.translate("directory.terabyte"),
        }
        var finalNb = bytes
        while(finalNb >= 1024 && order < 4){
            finalNb = finalNb / 1024
            order++
        }
        return {
            nb: finalNb,
            order: orders[order]
        }
    }
    $scope.formatStorage = function(filled, quota){
        var ratio = filled / quota
        var representation = $scope.getAppropriateDataUnit(quota)
        return (Math.round(representation.nb * ratio * 100) / 100) //2 digits
    }
    $scope.formatQuota = function(quota){
        var representation = $scope.getAppropriateDataUnit(quota)
        return (Math.round(representation.nb * 100) / 100)+" "+representation.order
    }
    $scope.getStorageRatio = function(storage, quota){
        return Math.min(100, Math.round((storage * 100) / quota * 100) / 100)
    }

    // Angular user styling (depends on its role / isolated)
    $scope.userStyle = function(user){
		return {
			'user-style': true,
			'teacher': user.type === "Teacher",
			'personnel': user.type === "Personnel",
			'relative': user.type === "Relative",
			'student': user.type === "Student",
			'guest': user.type === "Guest",
			'isolated': user.isolated,
			'not-active': user.code
		}
    }

    // Angular user list ordering
    $scope.typeOrdering = function(user){
        switch(user.type){
            case "Teacher":
                return 0
            case "Personnel":
                return 1
            case "Relative":
                return 2
            case "Student":
                return 3
        }
        return 100
    }
    $scope.userOrdering = ['lastName', $scope.typeOrdering]

    $scope.switchOrdering = function(){
        var temp = $scope.userOrdering[0]
        $scope.userOrdering[0] = $scope.userOrdering[1]
        $scope.userOrdering[1] = temp
    }

	$scope.setShowWhat = function(what){
		$scope.showWhat = what
	}

	$scope.fairInclusion = function(anyString, challenger){
		return lang.removeAccents(anyString.toLowerCase()).indexOf(lang.removeAccents(challenger.toLowerCase())) >= 0
	}

	//Show by default
	$scope.userFilters = {
		showIsolated : true,
		showInactive : true,
		showTeachers : true,
		showPersonnel : true,
		showRelative : true,
		showStudents : true,
		showGuests: true,
		showFeedModeManual: true,
		showFeedModeAuto: true
	}
	$scope.toggleFilter = function(filterName){
		$scope.userFilters[filterName] = !$scope.userFilters[filterName]
	}

    $scope.structureUserFilteringFunction = function(user){
		var filterByClass	 = user.classesList && user.classesList.length > 0
		var filterByInput 	 = $rootScope.filterStructureUsers ? $scope.fairInclusion(user.displayName, $rootScope.filterStructureUsers) : true
		var filterIsolated 	 = $scope.userFilters.showIsolated 	&& user.isolated
		var filterInactive	 = user.code 				 ? $scope.userFilters.showInactive  : true
		var filterTeachers 	 = user.type === 'Teacher' 	 ? $scope.userFilters.showTeachers 	: true
		var filterPersonnel  = user.type === 'Personnel' ? $scope.userFilters.showPersonnel : true
		var filterRelative 	 = user.type === 'Relative'  ? $scope.userFilters.showRelative 	: true
		var filterStudents 	 = user.type === 'Student' 	 ? $scope.userFilters.showStudents 	: true
		var filterGuests 	 = user.type === 'Guest' 	 ? $scope.userFilters.showGuests 	: true
		var filterFeedManual = user.source === "MANUAL"  ? $scope.userFilters.showFeedModeManual : true
		var filterFeedAuto   = user.source !== "MANUAL"  ? $scope.userFilters.showFeedModeAuto : true

        return filterByInput && (filterByClass || filterIsolated) &&
			filterInactive && filterTeachers && filterPersonnel &&
			filterRelative && filterStudents && filterGuests &&
			filterFeedAuto && filterFeedManual
	}
	$scope.isolatedUserFilteringFunction = function(input){
		return function(user){
			return (input && user.displayName) ? $scope.fairInclusion(user.displayName, input) : true
		}
	}
	$scope.groupUserFilteringFunction = function(input, classObj){
		return function(user){
			var filterByInput = input ? $scope.fairInclusion(user.displayName, input) : true
			var filterByClass = classObj ? _.find(user.totalClasses, function(classe){ return classe.id === classObj.id }) : true
			return filterByInput && filterByClass
		}
	}
	$scope.filterExcludeCurrentStructure = function(input){
		return function(structure){
			var excludeCurrent = structure.id !== $scope.structure.id
			var filterByInput = input ? $scope.fairInclusion(structure.name, input) : true
			return excludeCurrent && filterByInput
		}
	}
	$scope.filterOnlyParentStructures = function(structure){
		return  _.find(structure.children, function(c){ return c.id === $scope.structure.id })
	}
	$scope.filterOnlyChildStructures = function(structure){
		return  _.find(structure.parents, function(p){ return p.id === $scope.structure.id })
	}
	$scope.filterTopStructures = function(structure){
		return !structure.parents
	}
	$scope.filterCyclicChildren = function(structure){
		return !$scope.struct.parents.findWhere({id: structure.id})
	}
	$scope.filterExcludeDoubles = function(list){
		return function(item){
			return !_.contains(list, item)
		}
	}
	$scope.filterChildren = function(struct){
		if(struct === $scope.structure)
			return struct.children
			
		var parents = []

		var recursivelyTagParents = function(struct){
			if(typeof struct.parents === 'undefined')
				return

			_.forEach(struct.parents, function(p){
				if(parents.indexOf(p) < 0)
					parents.push(p)
				recursivelyTagParents(p)
			})
		}
		recursivelyTagParents(struct)

		//Prevents infinite loops when parents are contained inside children.
		return _.filter(struct.children, function(child){
			return !child.selected || !_.findWhere(parents, {id: child.id})
		})
	}
	$scope.selectOnly = function(structure, structureList){
		/*
			_.forEach(structure.children, function(s){ s.selected = false })
			_.forEach(structureList, function(s){ s.selected = s.id === structure.id ? true : false })
		*/
		$scope.structures.forEach(function(structure){
			structure.selected = false
		})

		var recursivelySelectParents = function(structure){
			//Prevent infinite loops
			if(structure.selected)
				return;

			structure.selected = true;

			if(!structure.parents)
				return;

			_.forEach(structure.parents, function(parent){
					var parentLocated = $scope.structures.findWhere({id: parent.id })
					if(parentLocated)
						recursivelySelectParents(parentLocated)
			})
		}
		recursivelySelectParents(structure)

	}
    ////////

	$scope.deleteIfEmpty = function(object, property){
		if(typeof object[property] !== 'undefined' && object[property].length < 1)
			delete object[property]
	}

	$scope.exportData = {
		export_mode: "all",
		classId : "",
		structureId : "",
		params: {}
	}
    $scope.export_mode = "all"

    $scope.openExport = function(){
		var exportData = $scope.exportData
        var where = 'export/users?format=csv'
		if(exportData.export_mode !== 'all'){
			where += "&" + exportData.export_mode + "=" + exportData[exportData.export_mode]
		}
		where += "&" + $.param(exportData.params)

        window.open(where, '_blank')
    }

	$scope.exportItem = function(item, mode, params){
		$scope.exportData.export_mode = mode
		$scope.exportData[mode] = item.id
		if(params)
			$scope.exportData.params = params
		$scope.openExport()
	}

	// Import CSV
	$scope.importCSVData = {
		profile : "",
		charset : ""
	}

	$scope.importCSV = function(structure){
		structure.importCSV($scope.importCSVData.csv[0], $scope.importCSVData.profile, $scope.importCSVData.charset)
	};

    //Refresh the isolated users list.
    $scope.isolatedUsers = model.isolatedUsers
    $scope.refreshIsolated = function(){
        model.isolatedUsers.sync()
        delete $scope.isolatedUser
    }

	//Refresh the cross users list.
	$scope.crossUsers = model.crossUsers
	$scope.refreshCrossUsers = function(filter){
        model.crossUsers.users.sync(filter)
        delete $scope.crossUser
    }

	$scope.refreshStructures = function(){
		$scope.structures.sync($scope.refreshScope)
	}

    $scope.getUserDetails = function(user){
		if(!user)
			return

        user.get(function(){
			if($scope.notCrossSearchTab() && user.type === 'Relative'){
				user.children = $scope.structure.users.filter(function(u){
					return _.findWhere(user.children, {id: u.id})
				})
			}
            $rootScope.quotaUnit = $scope.DEFAULT_QUOTA_UNIT
			$rootScope.quotaSize = user.quota / $scope.DEFAULT_QUOTA_UNIT
            $scope.refreshScope()
        }, true)
    }

    //Deep loading of a structure (classes + users + class flags on users) and view refresh
    $scope.viewStructure = function(structure){
        $scope.structure = structure
		structure.manualGroups.sync($scope.refreshScope)
		structure.duplicates.sync($scope.refreshScope)
        structure.loadStructure($scope.refreshScope, $scope.refreshScope)
    }

    $scope.reloadStructureAndRetrieveUser = function(user){
		if(!$scope.notCrossSearchTab()){
			return $scope.getUserDetails(user)
		}

        return function(){
            $scope.structure.loadStructure(
                $scope.refreshScope,
                function(){
                    $rootScope.targetUser = _.findWhere($scope.structure.users.all, {id: user.id})
					$scope.getUserDetails($rootScope.targetUser)
                }
            )
        }
    }

	$scope.reloadStructure = function(structure){
		return function(){ $scope.viewStructure(structure) }
	}

    //Refresh the classes list (inside a structure object)
    $scope.refreshClasses = function(structure){
        structure.sync($scope.refreshScope)
    }

    //Toggling used in the filtering menu.
    $scope.toggleClass = function(classe, structure){
        classe.selected = !classe.selected
        if(classe.selected)
            structure.addClassUsers(classe, [$scope.refreshScope])
        else
            structure.removeClassUsers(classe, $scope.refreshScope)
    }

    //Select all classes in the filtering screen.
    $scope.selectAllClasses = function(){
        _.forEach($scope.structure.users.all, function(user){
            user.classesList = user.totalClasses
        })
        $scope.structure.classes.selectAll()
    }

    //Deselect all classes in the filtering screen.
    $scope.deselectAllClasses = function(){
        _.forEach($scope.structure.users.all, function(user){
            user.classesList = []
        })
        $scope.structure.classes.deselectAll()
    }

	//Creates a new fresh User
	$scope.initUser = function(){
		var newUser = new User()
		$scope.resetUser(newUser)
		return newUser
	}

	$scope.resetUser = function(user){
		delete user.classId
		delete user.birthDate
		delete user.firstName
		delete user.lastName
		user.type = 'Personnel'
		user.structureId = $scope.structure.id
		user.children = []
	}

    //Init the date of birth. (cannot be put in html, causes angular parsing error)
    $scope.initUserBirth = function(user){
        user.birthDate = new Date()
    }

	$scope.createUser = function(user){
		user.create(function(){
			$scope.resetUser(user)
			$scope.structure.loadStructure($scope.refreshScope)
		})
	}

	$scope.updateUser = function(user){
		user.update(function(){ $scope.$apply() })
	}

	$scope.deleteUser = function(user){
		user.delete(function(){
			delete $scope.targetUser
			if($scope.notCrossSearchTab()){
				$scope.structure.users.remove(user)
			} else {
				$scope.crossUsers.users.remove(user)
			}
			$scope.$apply()
		})
	}

    //Batch quota update for all users in a single class.
    $scope.saveClassQuota = function(classe, size, profile){
        $http.get('user/admin/list', {
            params: {
                classId: classe.id,
				profile: profile
            }
        }).success(function(users){
			var userarray = _.map(users, function(user){
                return user.id
            })
            $http.put('/workspace/quota', {
                users: userarray,
                quota: size
            }).success(function(){
                notify.info(lang.translate("directory.notify.quotaUpdate"))
            })
        })
    }

	//Batch quota update for all users in a single structure.
	$scope.saveStructureQuota = function(structure, size, profile){
		$http.get('user/admin/list', {
            params: {
                structureId: structure.id,
				profile: profile
            }
        }).success(function(users){
			var userarray = _.map(users, function(user){
                return user.id
            })
            $http.put('/workspace/quota', {
                users: userarray,
                quota: size
            }).success(function(){
                notify.info(lang.translate("directory.notify.quotaUpdate"))
            })
        })
	}

	$scope.addChild = function(child, user){
		if(user.children.indexOf(child) < 0){
			user.children.push(child)

			if(!user.id)
				return

			user.linkChild(child).error(function(){
				var index = user.children.indexOf(child)
				if(index >= 0)
					user.children.splice(index, 1)
		    })
		}
	}
	$scope.removeChild = function(child, user){
		var index = user.children.indexOf(child)
		if(index >= 0){
			user.children.splice(index, 1)

			if(!user.id)
				return

			user.unlinkChild(child).error(function(){
				user.children.push(child)
			})
		}
	}

	//Groups
	$scope.initGroup = function(){
		var newGroup = new ManualGroup()
		return newGroup
	}

	$scope.refreshGroups = function(structure){
		structure.manualGroups.sync($scope.refreshScope)
	}
	$scope.saveGroup = function(group){
		group.save($scope.structure, function(){ $scope.refreshGroups($scope.structure)})
	}
	$scope.updateGroup = function(group){
		group.update(function(){ $scope.refreshGroups($scope.structure)})
	}
	$scope.deleteGroup = function(group){
		group.delete(function(){ $scope.refreshGroups($scope.structure)})
	}
	$scope.addUserToGroup = function(user, group){
		if(_.some(group.data.users, function(x){ return user.displayName === x.username }))
			return
		group.addUser(user, function(){
			group.getUsers($scope.refreshScope)
		})
	}
	$scope.removeUserFromGroup = function(user, group){
		group.removeUser(user, function(){
			group.getUsers($scope.refreshScope)
		})
	}

	//Structures
	$scope.initStructure = function(){
		$scope.createdStructure = new Structure()
	}

	//Duplicates
	$scope.refreshDuplicates = function(){
		$scope.structure.duplicates.sync($scope.refreshScope)
		$scope.reloadStructure($scope.structure)()
	}

	$scope.markDuplicates = function(){
		http().post('/directory/duplicates/mark').done(function(){
			notify.info('directory.notify.markDuplicates')
		})
	}

	$scope.mapDuplicateUser = function(duplicateUser){
		return $scope.structure.users.findWhere({id: duplicateUser.id})
	}
}
