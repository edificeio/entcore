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

function AdminDirectoryController($scope, $rootScope, $http, $route, template, model, date, route, httpWrapper){

	$scope.display = {
		filterStructureClasses: ''
	};
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
	$scope.phonePattern = new RegExp("^(00|\\+)?(?:[0-9] ?-?\\.?){6,14}[0-9]$")
    $scope.loadingWrapper = httpWrapper.wrap

	$scope.DEFAULT_QUOTA_UNIT = 1073741824
    $scope.saveQuotaDisabled = true;
	$scope.saveQuotaStructureDisabled = true;
	$scope.saveQuotaActivityDisabled = true;

	$scope.maxQuotas = {}
	//Get max quotas
	http().get("/workspace/quota/default").done(function(result){
		$scope.maxQuotas = result
	})
	$scope.getMaxUserQuota = function(user){
		if(!user.profiles || !user.profiles[0])
			return 0
		return _.findWhere($scope.maxQuotas, {name: user.profiles[0]}).maxQuota
	}
	$scope.getMaxProfileQuota = function(profile){
		if(!profile)
			return 0

		return _.findWhere($scope.maxQuotas, {name: profile}).maxQuota
	}

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
		if(!user.functions || user.functions.length === 0 || user.functions[0][0] === null)
			return

		var memo = []

		return _.foldl(user.functions, function(str, fun){
			if(memo.indexOf(fun[0]) > -1)
				return str
			memo.push(fun[0])

			var strFun = lang.translate(fun[0])
			if(fun[1].length > 0){
				strFun += ' [' + _.chain(fun[1]).map(function(id){
					return _.findWhere($scope.structures.all, {id : id}).name
				}).value().join(' / ') + ']'
			}

			return str ? str + ', ' + strFun : strFun
		}, "")
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
	$scope.getCurrentLeaf = function(){
		return _.findWhere($scope.leafMenu, { name: $scope.currentLeaf })
	}
	$scope.leafMenu = [
		{
			name: "userTab",
			text: lang.translate("directory.userOps"),
			templateName: 'admin-user-tab',
			onClick: function(){
				delete $scope.targetUser
				$scope.scrollOpts.reset()
			},
			onStructureClick: function(structure){
				$scope.viewStructure(structure)
			},
			requestName : "user-requests"
		},
		{
			name: "structureTab",
			text: lang.translate("directory.structureOps"),
			templateName: 'admin-structure-tab',
			onClick: function(){
				$scope.scrollOpts.reset()
				$scope.initExportData()
			},
			onStructureClick: function(structure){
				structure.quotaFilterProfile = "";
				structure.quotaFilterNbusers = 10;
				structure.quotaFilterPercentageLimit = 0;
				structure.quotaFilterSortBy = "sortpercentage";
				structure.quotaFilterOrderBy = "sortdcecreasing";
				$scope.structure = structure
			}
		},
		{
			name: "classTab",
			text: lang.translate("directory.classOps"),
			templateName: 'admin-class-tab',
			onClick: function(){
				$scope.scrollOpts.reset()
				$scope.initExportData()
			},
			onStructureClick: function(structure){
				$scope.structure = structure
				structure.classes.sync($scope.refreshScope)
			},
			requestName : "classes-request"
		},
		{
			name: "groupTab",
			text: lang.translate("directory.groupOps"),
			templateName: 'admin-group-tab',
			onClick: function(){ $scope.scrollOpts.reset() },
			onStructureClick: function(structure){
				$scope.viewStructure(structure)
				structure.manualGroups.sync($scope.refreshScope)
			},
			requestName : "groups-request"
		},
		{
			name: "exportTab",
			text: lang.translate("directory.export"),
			templateName: 'admin-export-tab',
			onClick: function(){
				$scope.scrollOpts.reset()
				$scope.initExportData()
			}
		},
		{
			name: "maintenanceTab",
			text: lang.translate("directory.feeding"),
			templateName: 'admin-maintenance-tab',
			onClick: function(){ $scope.scrollOpts.reset() },
			showCondition: function(){ return $scope.isCentralAdmin() }
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
			onStructureClick: function(structure){
				$scope.structure = structure
				structure.duplicates.sync($scope.refreshScope)
			},
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
		},
		{
			name: "paramsTab",
			text: lang.translate("directory.params.tab"),
			templateName: 'admin-params-tab',
			onClick: function(){
				$scope.scrollOpts.reset();
				$scope.refreshProfiles();
			},
			requestName : "profiles-request",
			showCondition: function(){ return $scope.isCentralAdmin() }
		}
	]
	_.forEach($scope.leafMenu, function(leaf){
		var temp = leaf.onClick
		leaf.onClick = function(){
			$scope.currentLeaf = leaf.name
			if(leaf.onStructureClick && $scope.structure){
				leaf.onStructureClick($scope.structure)
			}
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
    $scope.formatQuotaNumberOnly = function(quota, quotaUnit){
        return (Math.round(quota / quotaUnit) / 100);
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
		showFeedModeAuto: true,
		showLocalAdmin: false
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
		var filterLocalAdmin = $scope.userFilters.showLocalAdmin ? _.chain(user.functions).map(function(f){ return f[0]}).uniq().value().indexOf("ADMIN_LOCAL") >= 0 : true

        return filterByInput && (filterByClass || filterIsolated) &&
			filterInactive && filterTeachers && filterPersonnel &&
			filterRelative && filterStudents && filterGuests &&
			filterFeedAuto && filterFeedManual && filterLocalAdmin
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

	$scope.formats = [
		{
			key: "",
			label: 'directory.admin.export.type.default',
			format: 'csv'
		},
		{
			key: "Esidoc",
			label: 'directory.admin.export.type.esidoc',
            profiles: ['Teacher', 'Student', 'Personnel'],
			format: 'xml'
		},
		{
			key: "Cerise-teacher",
			label: 'directory.admin.export.type.cerise.teacher',
			format: 'csv',
            profiles: ['Teacher'],
			show: function(){
				return $scope.exportData.params.profile === 'Teacher'
			}
		},
		{
			key: "Cerise-student",
			label: 'directory.admin.export.type.cerise.student',
			format: 'csv',
            profiles: ['Student'],
			show: function(){
				return $scope.exportData.params.profile === 'Student'
			}
		},
		{
			key: "Sacoche",
			label: 'directory.admin.export.type.sacoche',
			format: 'csv'
		},
		{
			key: "Gepi",
			label: 'directory.admin.export.type.gepi',
			filename: 'ENT-Identifiants.csv',
			format: 'csv'
		},
		{
			key: "ProEPS-student",
			label: 'directory.admin.export.type.proeps.student',
			format: 'csv',
			profiles: ['Student'],
			show: function(){
				return $scope.exportData.params.profile === 'Student'
			}
		},
		{
			key: "ProEPS-relative",
			label: 'directory.admin.export.type.proeps.relative',
			format: 'csv',
			profiles: ['Relative'],
			show: function(){
				return $scope.exportData.params.profile === 'Relative'
			}
		}
	]
	$scope.initExportData = function(){
		$scope.exportData = {
			export_mode: "structureId",
			classId : "",
			structureId : "",
			filterFormat: function(format){
				return !format.show || format.show()
			},
            filterProfiles: function(profile){
                var format = this.findFormat(this.params.type)
                return !format.profiles || format.profiles.indexOf(profile) > -1
            },
			findFormat: function(key){
				return _.find($scope.formats, function(item){ return key === item.key })
			},
            onFormatChange: function(){
                var format = this.findFormat(this.params.type)
                if(format.profiles)
                    this.params.profile = format.profiles[0]
            },
			params: {
				type: "",
                profile: ""
			}
		}
	}
    $scope.initExportData()

    $scope.openExport = function(){
		var exportData = $scope.exportData
		var exportFormat = $scope.exportData.findFormat(exportData.params.type)
        var where = 'export/users?format=' + exportFormat.format
		if (!!exportFormat.filename) {
				where += "&filename" + "=" + exportFormat.filename
		}
		if(exportData.export_mode !== 'all'){
			where += "&" + exportData.export_mode + "=" + exportData[exportData.export_mode]
		}
        $scope.deleteIfEmpty(exportData.params, 'profile')
		where += "&" + $.param(exportData.params)

        window.open(where, '_blank')

        if(!exportData.params.profile)
            exportData.params.profile = ''
    }

	$scope.exportItem = function(item, mode, params){
		$scope.exportData.export_mode = mode
		$scope.exportData[mode] = item.id
		if(params)
			$scope.exportData.params = params
		$scope.openExport()
	}

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

	$scope.profiles = model.profiles.profiles;
	$scope.blockProfiles = { Personnel : false, Student : false, Teacher : false, Guest : false, Relative : false };
	$scope.refreshProfiles = function() {
		$scope.profiles.sync(function () {
			for (var i = 0; i < model.profiles.profiles.all.length; i++) {
				$scope.blockProfiles[model.profiles.profiles.all[i].name] = model.profiles.profiles.all[i].blocked;
			}
		});
	};
	$scope.blockLogin = function(blockProfiles) {
		model.profiles.save(blockProfiles);
	};

	$scope.refreshStructures = function(){
		$scope.structures.sync($scope.refreshScope)
	};

	$scope.filterByName = function(userClass){
		return lang.removeAccents(userClass.name).toLowerCase().indexOf(lang.removeAccents($scope.display.filterStructureClasses).toLowerCase()) !== -1;
	};

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
        structure.classes.sync($scope.refreshScope)
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

	//Save the quota for profiles
	$scope.saveStructureQuota = function() {
		$scope.structure.saveStructureQProfile($scope.structure);
	}

	//Save the quota for structure
	$scope.saveStructureQuotaStructure = function() {
		$scope.structure.saveStructureQStructure($scope.structure);
	}

	//Save the quota for structure in activity table
	$scope.saveStructureQuotaActivity = function() {
		$scope.structure.saveStructureQActivity($scope.structure);
	}

	// gettng the lines of quota activity table
	$scope.getUsersQuotaActivity = function() {
		$scope.structure.getUsersQuotaActivity(
										$scope.structure.id,
										$scope.structure.quotaFilterNbusers,
										$scope.structure.quotaFilterSortBy,
										$scope.structure.quotaFilterOrderBy,
										$scope.structure.quotaFilterProfile,
										$scope.structure.quotaFilterPercentageLimit,
										function(data) {
				$scope.structure.quotaActivity = data;
				
				$scope.$apply($scope.structure.quotaActivity);
			});
	}

    // when unit is modified in the input from the table
    $scope.updateQuotaUnit = function(buttonId, index, quota, maxquota, quotaUnit){

		if( buttonId == 1 ) {
			// update the quota in the profile list
			if ($scope.structure.pgroup[index].quotaOri == null) {
				$scope.structure.pgroup[index].quotaOri = $scope.structure.pgroup[index].quota * $scope.structure.pgroup[index].unit;
				$scope.structure.pgroup[index].quota = quota * $scope.structure.pgroup[index].unit;
			} else {
				$scope.structure.pgroup[index].quota = $scope.structure.pgroup[index].quotaOri;
			}

			// update the maxquota
			if ($scope.structure.pgroup[index].maxquotaOri == null || $scope.structure.pgroup[index].maxquotaOri == 0) {
				$scope.structure.pgroup[index].maxquotaOri = $scope.structure.pgroup[index].maxquota * $scope.structure.pgroup[index].unit;
				$scope.structure.pgroup[index].maxquota = maxquota * $scope.structure.pgroup[index].unit;
			} else {
				$scope.structure.pgroup[index].maxquota = $scope.structure.pgroup[index].maxquotaOri;
			}

			$scope.structure.pgroup[index].unit = quotaUnit;
			$scope.structure.pgroup[index].quota = Math.round(100 * $scope.structure.pgroup[index].quota / $scope.structure.pgroup[index].unit) / 100;
			$scope.structure.pgroup[index].maxquota = Math.round(100 * $scope.structure.pgroup[index].maxquota / $scope.structure.pgroup[index].unit) / 100;

		} else if( buttonId == 2 ) {

			// management of whole structure quota (1 field)
			if ($scope.structure.quotaOri == null ) {
				$scope.structure.quotaOri = $scope.structure.quota * $scope.structure.unit;
				$scope.structure.quota = quota * $scope.structure.unit;
			} else {
				$scope.structure.quota = $scope.structure.quotaOri;
			}
			$scope.structure.unit = quotaUnit;
			$scope.structure.quota = Math.round(100 * $scope.structure.quota / $scope.structure.unit) / 100;

		} else if( buttonId == 3 ) {

				// update the quota in the activity list
			if ($scope.structure.quotaActivity[index].quotaOri == null) {
				$scope.structure.quotaActivity[index].quotaOri = $scope.structure.quotaActivity[index].quota * $scope.structure.quotaActivity[index].unit;
				$scope.structure.quotaActivity[index].quota = quota * $scope.structure.quotaActivity[index].unit;
			} else {
				$scope.structure.quotaActivity[index].quota = $scope.structure.quotaActivity[index].quotaOri;
			}

			// update the storage
			if ($scope.structure.quotaActivity[index].storageOri == null) {
				$scope.structure.quotaActivity[index].storageOri = $scope.structure.quotaActivity[index].storage * $scope.structure.quotaActivity[index].unit;
				$scope.structure.quotaActivity[index].storage = storage * $scope.structure.quotaActivity[index].unit;
			} else {
				$scope.structure.quotaActivity[index].storage = $scope.structure.quotaActivity[index].storageOri;
			}
			// update the maxquota
			if ($scope.structure.quotaActivity[index].maxquotaOri == null || $scope.structure.quotaActivity[index].maxquotaOri == 0) {
				$scope.structure.quotaActivity[index].maxquotaOri = $scope.structure.quotaActivity[index].maxquota * $scope.structure.quotaActivity[index].unit;
				$scope.structure.quotaActivity[index].maxquota = maxquota * $scope.structure.quotaActivity[index].unit;
			} else {
				$scope.structure.quotaActivity[index].maxquota = $scope.structure.quotaActivity[index].maxquotaOri;
			}

			$scope.structure.quotaActivity[index].unit = quotaUnit;
			$scope.structure.quotaActivity[index].quota = Math.round(100 * $scope.structure.quotaActivity[index].quota / $scope.structure.quotaActivity[index].unit) / 100;
			$scope.structure.quotaActivity[index].storage = Math.round(100 * $scope.structure.quotaActivity[index].storage / $scope.structure.quotaActivity[index].unit) / 100;
			$scope.structure.quotaActivity[index].maxquota = Math.round(100 * $scope.structure.quotaActivity[index].maxquota / $scope.structure.quotaActivity[index].unit) / 100;
		}
    }

	// when modifying allocated space field for profiles
	$scope.setQuotaModified = function(index){
		// verify that the new value is under the maxquota value
		if( $scope.structure.pgroup[index].quota > $scope.structure.pgroup[index].maxquota && $scope.structure.pgroup[index].maxquota != 0) {
			// put back the old value (quotaOri)
			notify.error('directory.notify.maxquota.exceeded');
			$scope.structure.pgroup[index].quota = Math.round(100 * $scope.structure.pgroup[index].quotaOri / $scope.structure.pgroup[index].unit) /100;
		}else {
			// we save the value in 2 places : 1 is the displayed in the field (rounded), the other is the not rounded
			$scope.structure.pgroup[index].quotaOri = $scope.structure.pgroup[index].quota * $scope.structure.pgroup[index].unit;
			$scope.saveQuotaDisabled = false;
		}
	}

	// when modifying allocated space field in structure
	$scope.setQuotaStructureModified = function(){
		$scope.structure.quotaOri = $scope.structure.quota * $scope.structure.unit;
		$scope.saveQuotaStructureDisabled = false;
	}

	// when modifying allocated space field in activity
	$scope.setQuotaActivityModified = function(index){
		// verify that the new value is under the maxquota value
		if( $scope.structure.quotaActivity[index].quota > $scope.structure.quotaActivity[index].maxquota && $scope.structure.quotaActivity[index].maxquota != 0) {
			// put back the old value (quotaOri)
			notify.error('directory.notify.maxquota.exceeded');
			$scope.structure.quotaActivity[index].quota = Math.round(100 * $scope.structure.quotaActivity[index].quotaOri / $scope.structure.quotaActivity[index].unit) /100;
		}else {
			// we save the value in 2 places : 1 is the displayed in the field (rounded), the other is the not rounded
			$scope.structure.quotaActivity[index].quotaOri = $scope.structure.quotaActivity[index].quota * $scope.structure.quotaActivity[index].unit;
			$scope.saveQuotaActivityDisabled = false;
		}
	}
	
	// when modifying maximum authorized space field
    $scope.setMaxQuotaModified = function(index){
        // we save the value in 2 places : 1 is the displayed in the field (rounded), the other is the not rounded
        $scope.structure.pgroup[index].maxQuotaOri = $scope.structure.pgroup[index].maxquota * $scope.structure.pgroup[index].unit;
        $scope.saveQuotaDisabled = false;
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

    $scope.excludeChildren = function(user){
        return function(child){
            return user.children.indexOf(child) < 0
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
		if(_.some(group.data.users, function(x){ return user.id === x.id }))
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

	$scope.removeUserFromStructure = function(user, structure, hook){
		new Structure(structure).unlinkUser(user, hook)
	}

    $scope.filterDuplicate = function(duplicate){
        return duplicate.score >= 4
    }

	/* Mass mailing */
	$scope.profileList = [{label: 'Student'}, {label: 'Relative'}, {label: 'Personnel'}, {label: 'Teacher'}, {label: 'Guest'}]
	_.forEach($scope.profileList, function(profile){
		profile.toString = function(){ return lang.translate(this.label) }
		profile.translatedLabel = lang.translate(profile.label)
	})

	$scope.massmail = {
		/* Modification flag */
		modified: false,
		modify: function(){
			$scope.massmail.modified = true
		},
		/* Utility methods */
		removeElement: function(prop , item){
			$scope.massmail[prop].splice($scope.massmail[prop].indexOf(item), 1)
			$scope.massmail.modify()
		},
		downloadAnchor: null,
		downloadObjectUrl: null,
		createDownloadAnchor: function(){
			$scope.massmail.downloadAnchor = document.createElement('a')
			$scope.massmail.downloadAnchor.style = "display: none"
			document.body.appendChild($scope.massmail.downloadAnchor)
		},
		ajaxDownload: function(blob, filename){
			if(window.navigator.msSaveOrOpenBlob) {
				//IE specific
	            window.navigator.msSaveOrOpenBlob(blob, filename);
        	} else {
				//Other browsers
				if($scope.massmail.downloadAnchor === null)
					$scope.massmail.createDownloadAnchor()
				if($scope.massmail.downloadObjectUrl !== null)
					window.URL.revokeObjectURL($scope.massmail.downloadObjectUrl);

				$scope.massmail.downloadObjectUrl = window.URL.createObjectURL(blob)
				var anchor = $scope.massmail.downloadAnchor
				anchor.href = $scope.massmail.downloadObjectUrl
				anchor.download = filename
				anchor.click()
			}
		},
		/* Single structure */
		structure: [],
		getStructure: function(){
			return this.structure.length === 1 ? this.structure[0] : null;
		},
		/* Profile list */
		profiles: [],
		/* Level list */
		levels: [],
		isPartialLevel: function(level){
			level.partial = _.find(level.classes, function(classe){ return _.findWhere($scope.massmail.classes, {id: classe.id}) })
			return level.partial
		},
		/* Class list */
		classes: [],
		/* Activation */
		activated: "false",
		/* Sort by */
		sortmethods: [
			{
				label: 'profile',
				translatedLabel: lang.translate('directory.admin.profile')
			},
			{
				label: 'classname',
				translatedLabel: lang.translate('directory.classe')
			}
		],
		sort1: "",
		sort2: "",
		filtermethod2: function(item){
			return $scope.massmail.sort1 !== item.label
		},
		/* User list */
		userList: [],
		userLimit: 20,
		countUsers: function(){
			return $scope.massmail.userList.length
		},
		countUsersWithoutMail: function(){
			return _.filter($scope.massmail.userList, function(u){ return !u.email }).length
		},
		resetUserLimit: function(){
			$scope.massmail.userLimit = 50
		},
		incrementUserLimit: function(){
			$scope.massmail.userLimit += 50
		},
		userOrder: '',
		setUserOrder: function(order){
			$scope.massmail.userOrder = $scope.massmail.userOrder === order ? '-' + order : order;
		},
		fetchingUsers: false,
		fetchUsers: function(){
			var that = $scope.massmail
			that.fetchingUsers = true

			var sortArray =
				that.sort1 && that.sort2 ? [that.sort1, that.sort2] :
				that.sort1 ? [that.sort1] :
				[]
			var levelClassesId = _.chain(that.levels)
				.filter(function(l){ return !l.partial })
				.pluck('classes')
				.flatten()
				.pluck('id')
				.value()
			var joinClassesAndLevels = _.pluck(that.classes, 'id').concat(
				_.chain(that.getStructure().classes.all)
					.filter(function(c){ return levelClassesId.indexOf(c.id) >= 0 })
					.pluck('id')
					.value()
			)
			$http.get('/directory/structure/'+that.getStructure().id+'/massMail/users', {
				params: {
					a: that.activated,
					p: _.map(that.profiles, function(p){ return p.label }),
					//l: _.chain(that.levels).filter(function(l){ return !l.partial }).map(function(l){ return l.name }).value(),
					c: joinClassesAndLevels,
					s: sortArray
				}
			}).success(function(data){
				that.modified = false
				that.userList = data
				_.forEach(that.userList, function(user){
					user.translatedProfile = lang.translate(user.profile)
					user.classesStr = user.classes.join(" ")
				})
				that.resetUserLimit()
				that.fetchingUsers = false
			}).error(function(data, status){
				notify.error('e'+status)
				that.fetchingUsers = false
			})
		},
		closeUserList: function(){
			$scope.massmail.userList = []
		},
		sortObject: {
			lastName: '',
			firstName: '',
			translatedProfile: '',
			classesStr: ''
		},
		userToString: function(u){
			var result = ""
			var stringProperties = ["lastName", "firstName", "profile", "login", "activationCode", "email"]
			var arrayProperties = ["classes"]
			var i
			for(i = 0; i < stringProperties.length; i++){
				if(i > 0)
					result += ";"
				result += u[stringProperties[i]]
			}
			for(i = 0; i < arrayProperties.length; i++){
				result += ";" + u[arrayProperties[i]].join(',')
			}
			return result
		},
		exportCSV: function(){
			var csvHeader = ""
			var bom = "\ufeff"
			var i18nArray = ["directory.admin.name", "directory.firstName", "directory.admin.profile", "directory.userLogin", "directory.userCode", "directory.admin.email", "directory.classes"]
			for(var i = 0; i < i18nArray.length; i++){
				if(i > 0)
					csvHeader += ";"
				csvHeader += lang.translate(i18nArray[i])
			}
			var csvString = bom + csvHeader + _.map($scope.massmail.userList, function(u){ return "\r\n" + $scope.massmail.userToString(u) }).join("")
			$scope.massmail.ajaxDownload(new Blob([csvString]), lang.translate("directory.massmail.filename")+".csv")
		},
		/* Massmail processing */
		processing: false,
		process: function(type){
			var that = $scope.massmail
			that.processing = true
			var sortArray =
				that.sort1 && that.sort2 ? [that.sort1, that.sort2] :
				that.sort1 ? [that.sort1] :
				[]
			var levelClassesId = _.chain(that.levels)
				.filter(function(l){ return !l.partial })
				.pluck('classes')
				.flatten()
				.pluck('id')
				.value()
			var joinClassesAndLevels = _.pluck(that.classes, 'id').concat(
				_.chain(that.getStructure().classes.all)
					.filter(function(c){ return levelClassesId.indexOf(c.id) >= 0 })
					.pluck('id')
					.value()
			)
			if(type === 'pdf'){
				$http.get('/directory/structure/'+that.getStructure().id+'/massMail/process/pdf', {
					params: {
						a: that.activated,
						p: _.map(that.profiles, function(p){ return p.label }),
						//l: _.chain(that.levels).filter(function(l){ return !l.partial }).map(function(l){ return l.name }).value(),
						c: joinClassesAndLevels,
						s: sortArray,
						filename: lang.translate("directory.massmail.filename")
					},
					responseType: 'blob'
				}).success(function(blob){
					$scope.massmail.ajaxDownload(blob, lang.translate("directory.massmail.filename")+".pdf")
					that.processing = false
				}).error(function(data, status){
					notify.error('e'+status)
					that.processing = false
				})
			} else if(type === 'mail'){
				$http.get('/directory/structure/'+that.getStructure().id+'/massMail/process/mail', {
					params: {
						a: that.activated,
						p: _.map(that.profiles, function(p){ return p.label }),
						//l: _.chain(that.levels).filter(function(l){ return !l.partial }).map(function(l){ return l.name }).value(),
						c: joinClassesAndLevels,
						filename: lang.translate("directory.massmail.filename")
					},
					responseType: 'json'
				}).success(function(json){
					notify.success("directory.massmail.mail.done")
					that.processing = false
				}).error(function(data, status){
					notify.error('e'+status)
					that.processing = false
				})
			}

		}
	}

	$scope.$watchCollection('massmail.structure', function(){
		if($scope.massmail.structure.length == 1){
			$scope.massmail.structure[0].classes.sync()
			$scope.massmail.structure[0].levels.sync()
		}
	})

	$scope.comboLabels = {
		options: lang.translate('options'),
		searchPlaceholder: lang.translate('search'),
		selectAll: lang.translate('select.all'),
		deselectAll: lang.translate('deselect.all')
	}

	$scope.filterClasses = function(classe){
		if($scope.massmail.levels.length === 0)
			return true

		for(var i = 0; i < $scope.massmail.levels.length; i++){
			if(_.findWhere($scope.massmail.levels[i].classes, {id: classe.id}))
				return true
		}
	}

    $scope.isMainStructure = function(user, structure){
        return _.findWhere(user.administrativeStructures, {id: structure.id})
    }

    $scope.isADMC = function() {
        return model.me.functions['SUPER_ADMIN'];
    };
}
