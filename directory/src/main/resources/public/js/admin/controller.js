routes.define(function($routeProvider){
	$routeProvider
		.when('/structureUser/:structureId/:userId', {
			action: 'viewStructureUser'
		})
        .when('/class/:structureId/:classId', {
            action: 'viewClass'
        })
		.when('/classUsers/:structureId/:classId', {
			action: 'viewClassUsers'
		})
})

function AdminDirectoryController($scope, $rootScope, $http, $route, template, model, date, route){

    route({
        viewStructureUser: function(params){
            var userId = params.userId
            var structureId = params.structureId

			template.open('body', 'admin-user-tab')
			$scope.showWhat = 'showStructureUser'

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.reloadStructureAndRetrieveUser({ id: userId })()
			})
        },
        viewClass: function(params){
            var classId = params.classId
            var structureId = params.structureId

			template.open('body', 'admin-class-tab')

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})

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
			$scope.showWhat = 'showStructureUser'

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.structure.loadStructure(
					null,
					function(){
						$scope.showIsolated = false
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

	$route.reload()
	$scope.template = template
    $rootScope.export_id = ""
    $scope.export_mode = "all"
    $scope.structures = model.structures
    $scope.lang = lang

    $scope.SCROLL_INCREMENT = 250
    $scope.usersLimit = $scope.SCROLL_INCREMENT
    $scope.incrementUsersLimit = function(){
        $scope.usersLimit += $scope.SCROLL_INCREMENT
    }

    $scope.refreshScope = function(){ $scope.$apply() }

    $scope.formatLongDate = function(d){
        return d ? date.create(d).format('LLLL') : ""
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

	$scope.leafMenu = [
		{
			text: lang.translate("directory.userOps"),
			templateName: 'admin-user-tab',
			onClick: function(){ },
			requestName : "user-requests"
		},
		{
			text: lang.translate("directory.structureOps"),
			templateName: 'admin-structure-tab',
			onClick: function(){ },
			showCondition: function(){ return $scope.isCentralAdmin() }
		},
		{
			text: lang.translate("directory.classOps"),
			templateName: 'admin-class-tab',
			onClick: function(){ }
		},
		{
			text: lang.translate("directory.groupOps"),
			templateName: 'admin-group-tab',
			onClick: function(){ },
			requestName : "groups-requests"
		},
		{
			text: lang.translate("directory.feeding"),
			templateName: 'admin-maintenance-tab',
			onClick: function(){ }
		},
		{
			text: lang.translate("directory.isolatedUsers"),
			templateName: 'admin-isolated-tab',
			onClick: function(){ $scope.refreshIsolated() },
			requestName : "isolated-request",
			showCondition: function(){ return $scope.isCentralAdmin() }
		}
	]

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
    $scope.showIsolated 	= true
	$scope.showInactive 	= true
	$scope.showTeachers		= true
	$scope.showPersonnel	= true
	$scope.showRelative		= true
	$scope.showStudents		= true
    $scope.structureUserFilteringFunction = function(user){
		var filterByClass	 = user.classesList && user.classesList.length > 0
		var filterByInput 	 = $rootScope.filterStructureUsers ? $scope.fairInclusion(user.displayName, $rootScope.filterStructureUsers) : true
		var filterIsolated 	 = $scope.showIsolated 	&& user.isolated
		var filterInactive	 = user.code 				 ? $scope.showInactive  : true
		var filterTeachers 	 = user.type === 'Teacher' 	 ? $scope.showTeachers 	: true
		var filterPersonnel  = user.type === 'Personnel' ? $scope.showPersonnel : true
		var filterRelative 	 = user.type === 'Relative'  ? $scope.showRelative 	: true
		var filterStudents 	 = user.type === 'Student' 	 ? $scope.showStudents 	: true

        return filterByInput && (filterByClass || filterIsolated) && filterInactive && filterTeachers && filterPersonnel && filterRelative && filterStudents
	}
	$scope.isolatedUserFilteringFunction = function(user){
		return ($scope.filterIsolatedUsers && user.displayName) ? $scope.fairInclusion(user.displayName, $scope.filterIsolatedUsers) : true
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
	$scope.selectOnly = function(structure, structureList){
		_.forEach(structure.children, function(s){ s.selected = false })
		_.forEach(structureList, function(s){ s.selected = s.id === structure.id ? true : false })
	}
    ////////

    //Starts the download in a new tab.
    $scope.openExport = function(export_mode, export_id){
        var where = 'export/users?format=csv'
        where += export_mode !== 'all' ? "&"+export_mode+"="+export_id : ""
        window.open(where, '_blank')
    }

    //Refresh the isolated users list.
    $scope.isolatedUsers = model.isolatedUsers
    $scope.refreshIsolated = function(){
        model.isolatedUsers.sync()
        delete $scope.isolatedUser
    }

	$scope.refreshStructures = function(){
		$scope.structures.sync($scope.refreshScope)
	}

    $scope.getUserDetails = function(user){
        user.get(function(){
			if(user.type === 'Relative'){
				user.children = $scope.structure.users.filter(function(u){
					return _.findWhere(user.children, {id: u.id})
				})
			}
            $rootScope.quotaSize = user.quota
            $rootScope.quotaUnit = 1
            $scope.refreshScope()
        }, true)
    }

    //Deep loading of a structure (classes + users + class flags on users) and view refresh
    $scope.viewStructure = function(structure){
        $scope.structure = structure
		structure.manualGroups.sync($scope.refreshScope)
        structure.loadStructure($scope.refreshScope)
    }

    $scope.reloadStructureAndRetrieveUser = function(user){
        return function(){
            $scope.structure.loadStructure(
                $scope.refreshScope,
                function(){
                    $rootScope.structureUser = _.findWhere($scope.structure.users.all, {id: user.id})
					$scope.getUserDetails($rootScope.structureUser)
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
		newUser.type 		= 'Personnel'
		newUser.structureId = $scope.structure.id
		newUser.children = []
		return newUser
	}

    //Init the date of birth. (cannot be put in html, causes angular parsing error)
    $scope.initUserBirth = function(user){
        user.birthDate = new Date()
    }

	$scope.createUser = function(user){
		user.create(function(){
			$scope.structure.loadStructure($scope.refreshScope)
		})
	}

    //Batch quota update for all users in a single class.
    $scope.saveClassQuota = function(classe, size, profile){
        $http.get('user/admin/list', {
            params: {
                classId: classe.id
            }
        }).success(function(users){
            var userarray = _.filter(users, function(user){
                return user.type === profile
            })
            userarray = _.map(userarray, function(user){
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
}
