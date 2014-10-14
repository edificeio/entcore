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

function AdminDirectoryController($scope, $rootScope, $http, model, date, route){

    route({
        viewStructureUser: function(params){
			if($scope.selected !== undefined)
				return

            var userId = params.userId
            var structureId = params.structureId

            $scope.selected = 0
			$scope.showWhat = 'showStructureUser'

			$scope.structures.sync(function(){
				$scope.structure = $scope.structures.find(function(structure){
					return structure.id === structureId
				})
				$scope.reloadStructureAndRetrieveUser({ id: userId })()
			})
        },
        viewClass: function(params){
			if($scope.selected !== undefined)
				return

            var classId = params.classId
            var structureId = params.structureId

            $scope.selected = 3
            $scope.structure = $scope.structures.find(function(structure){
                return structure.id === structureId
            })

			$scope.structures.sync(function(){
	            $scope.structure.classes.sync(function(){
	                $scope.classSelected = $scope.structure.classes.findWhere({ id: classId})
	                $scope.$apply()
	            })
			})
        },
		viewClassUsers: function(params){
			if($scope.selected !== undefined)
				return

			var classId = params.classId
			var structureId = params.structureId

			$scope.selected = 0
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
    $scope.userOrdering = ['displayName', $scope.typeOrdering]

    $scope.switchOrdering = function(){
        var temp = $scope.userOrdering[0]
        $scope.userOrdering[0] = $scope.userOrdering[1]
        $scope.userOrdering[1] = temp
    }

    $scope.showIsolated = true
    $scope.structureUserFilteringFunction = function(user){
        return (    user.classesList &&
                    user.classesList.length > 0 &&
                    ($rootScope.filterStructureUsers ? user.displayName.toLowerCase().indexOf($rootScope.filterStructureUsers.toLowerCase()) >= 0 : true)
                ) || ($scope.showIsolated && user.isolated && ($rootScope.filterStructureUsers ? user.displayName.toLowerCase().indexOf($rootScope.filterStructureUsers.toLowerCase()) >= 0 : true))
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

    $scope.getUserDetails = function(user){
        user.get(function(){
            $rootScope.quotaSize = user.quota
            $rootScope.quotaUnit = 1
            $scope.refreshScope()
        }, true)
    }

    //Deep loading of a structure (classes + users + class flags on users) and view refresh
    $scope.viewStructure = function(structure){
        $scope.structure = structure;
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
		if(user.children.indexOf(child) < 0)
			user.children.push(child)
	}
	$scope.removeChild = function(child, user){
		var index = user.children.indexOf(child)
		if(index >= 0)
			user.children.splice(index, 1)
	}
}
