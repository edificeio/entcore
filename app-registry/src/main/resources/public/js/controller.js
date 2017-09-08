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
function AppRegistry($scope, $sce, model, template, httpWrapper){

	/////// VARS ///////
	$scope.template = template
	$scope.lang = lang
    $scope.loadingWrapper = httpWrapper.wrap

	$scope.applications = model.applications
	$scope.roles = model.roles
	$scope.schools = model.schools
    $scope.widgets = model.widgetApps

    /////// TOP NOTIFICATIONS ///////
    $scope.topNotification = {
        show: false,
        message: "",
        confirm: null
    }
    $scope.notifyTop = function(text, action){
        $scope.topNotification.message = "<p>"+text+"</p>"
        $scope.topNotification.confirm = action
        $scope.topNotification.show = true
    }
    $scope.colourText = function(text){
        return '<span class="colored">'+text+'</span>'
    }

	/////// THEMES ///////

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

	/////// MENU ////////

	$scope.filterLeafMenuItems = function(item, index){
		return typeof item.showCondition !== "function" || item.showCondition()
	}
	$scope.isCentralAdmin = function(){
		return _.findWhere(model.me.functions, {code: "SUPER_ADMIN"}) !== undefined
	}
	$scope.isAdminLocal = function(){
		return _.findWhere(model.me.functions, {code: "ADMIN_LOCAL"}) !== undefined
	}

	$scope.currentLeaf = ""
	$scope.getCurrentLeaf = function(){
		return _.findWhere($scope.leafMenu, { name: $scope.currentLeaf })
	}
	$scope.leafMenu = [
		{
			name: "appParam",
			text: lang.translate("appregistry.appParam"),
			templateName: 'admin-app-param',
			onClick: function(){},
			showCondition: function(){ return $scope.isCentralAdmin() }
		},
		{
			name: "appRoles",
			text: lang.translate("appregistry.appRoles"),
			templateName: 'admin-app-roles',
			onClick: function(){
				 $scope.setCrossRoles($scope.flagCrossRoles())
				 $scope.viewRole(null)
			},
			showCondition: function(){ return $scope.isCentralAdmin() }
		},
		{
			name: "structureTab",
			text: lang.translate("appregistry.crossRoles"),
			templateName: 'admin-cross-roles',
			onClick: function(){
				$scope.setCrossRoles($scope.flagCrossRoles())
				$scope.newRole()
			},
			showCondition: function(){ return $scope.isCentralAdmin() }
		},
		{
			name: "roleAttribution",
			text: lang.translate("appregistry.roleAttribution"),
			templateName: 'admin-role-attribution',
			onClick: function(){
				$scope.setCrossRoles($scope.flagCrossRoles())
			},
			selectSchoolAction: function(school){
				school.groups.sync($scope.$apply)
			},
			showCondition: function(){ return $scope.isCentralAdmin() }
		},
		{
			name: "externalApps",
			text: lang.translate("appregistry.external.apps"),
			templateName: 'admin-external-apps',
			onClick: function(){},
			selectSchoolAction: function(school){
				school.syncExternalApps(function(){
                    if(!$scope.externalApp || !_.find($scope.school.externalApplications, function(app){ return app.data.id === $scope.externalApp.data.id})){
                        template.open("externalAppView", "admin-external-apps-list")
                        $scope.$apply()
                    } else if($scope.showPanel() === 'massAttribution' && !school.children){
                        $scope.showPanel('attribution')
                    }
                })
                school.groups.sync(function(){
                    if($scope.externalApp && _.find($scope.school.externalApplications, function(app){ return app.data.id === $scope.externalApp.data.id})){
                        $scope.linkedGroupsOpts.reorderGroups()
                        $scope.$apply()
                    }
                })
                if(!template.containers.externalAppView || template.containers.externalAppView == "empty")
				    template.open("externalAppView", "admin-external-apps-list")
			}
		},
		{
			name: "widgets",
			text: lang.translate("appregistry.widgets"),
			templateName: 'admin-widgets',
			onClick: function(){},
            selectSchoolAction: function(school){
                school.groups.sync(function(){
                    if($scope.widget){
                        $scope.getWidget(school.id)
                    }
                })
            }
		}
	]
	_.forEach($scope.leafMenu, function(leaf){
		var temp = leaf.onClick
		leaf.onClick = function(){
			$scope.currentLeaf = leaf.name
			if(leaf.selectSchoolAction && $scope.school){
				leaf.selectSchoolAction($scope.school)
			}
			temp()
		}
	})

	/////// SCHOOLS ///////

	$scope.setSchool = function(school){
		if($scope.getCurrentLeaf().selectSchoolAction)
			$scope.getCurrentLeaf().selectSchoolAction(school)
		$scope.school = school
	}
	$scope.filterTopStructures = function(structure){
		return !structure.parents
	}
	$scope.selectOnly = function(structure, structureList){
		_.forEach(structure.children, function(s){ s.selected = false })
		_.forEach(structureList, function(s){ s.selected = s.id === structure.id ? true : false })
	}
    $scope.schools.on('sync', function(){
        $scope.schools.parentStructures = $scope.schools.filter(function(s){ return s.children })
    })

	/////// APPLICATIONS ///////
	http().get('/appregistry/cas-types').done(function(data){
        $scope.casTypes = data
    })
    $scope.casDescription = function(casType){
        return _.findWhere($scope.casTypes, {id: casType}).description
    }

	$scope.application = new Application({ name: 'Application', displayName: 'application', external: true })

	$scope.viewApplication = function(application){
		$scope.role = undefined
		$scope.application = application
		$scope.updatePath()
		$scope.application.open()
		$scope.application.on('change', function(){
			$scope.updatePath()
			$scope.$apply('application')
		})
	}

	$scope.newApplication = function(){
		$scope.application = new Application({ name: 'Application', displayName: 'application', external: true })
		$scope.updatePath()
		$scope.application.on('change', function(){
			$scope.updatePath()
			if(!$scope.$$phase){
				$scope.$apply('application')
			}
		})
	}

	$scope.deleteApplication = function(){
        var app = $scope.application
        var action = function(){
            app.delete(function(){
                if($scope.application === app)
                    delete $scope.application
                $scope.applications.remove(app)
                $scope.$apply()
            })
        }
        $scope.notifyTop(lang.translate('appregistry.confirm.app.deletion') + ' ' + $scope.colourText(app.displayName) + '.', action)
	}

	$scope.showAdvanced = function(){
		$scope.display.advanced = true
	}

	$scope.hideAdvanced = function(){
		$scope.display.advanced = false
	}

	var previewPath = ''
	$scope.display = {
		advanced: false
	}
	$scope.updatePath = function(){
		var path = $scope.application.address
		if($scope.application.target === 'adapter'){
			path = '/adapter#' + path
		}
		previewPath = $sce.trustAsResourceUrl('/appregistry/app-preview?displayName=' + lang.translate($scope.application.displayName) + '&icon=' + encodeURIComponent($scope.application.icon) + '&target=' + $scope.application.target + '&path=' + encodeURIComponent(path))
		if(!$scope.$$phase){
			$scope.$apply('application')
		}
	}
	$scope.updatePath()

	$scope.previewPath = function(){
		return previewPath
	}

	$scope.refreshPreview = function(){
		$('#previewFrame').attr('src', $('#previewFrame').attr('src')+'')
	}

	$scope.setUserinfoScope = function(application){
		if(!application)
			application = $scope.application
		if((!application.scope || application.scope.indexOf('userinfo') === -1)  && application.transferSession){
			application.scope = 'userinfo' + (application.scope || '')
		}
		if(application.scope && application.scope.indexOf('userinfo') !== -1 && !application.transferSession){
			application.scope = application.scope.replace('userinfo', '')
		}
	}

    $scope.switchCas = function(application){
        if(application.hasCas){
            application.casType = application.casType ? application.casType : "UidRegisteredService"
        } else {
            delete application.pattern
            delete application.casType
        }
    }

	/////// EXTERNAL APPS ///////
    $scope.showPanelValue = ''
    $scope.showPanel = function(panelValue){
        if(!panelValue){
            return $scope.showPanelValue
        } else {
            $scope.showPanelValue = panelValue
        }
    }

	$scope.showExternalAppList = function(){
		delete $scope.externalApp
		template.open("externalAppView", "admin-external-apps-list")
	}

	$scope.selectExternalApp = function(app){
		$scope.externalApp = app
		template.open("externalAppView", "admin-external-app-detail")
	}
    $scope.initExternalApp = function(){
        $scope.externalApp = new ExternalApplication()
        $scope.externalApp.data = { structureId: $scope.school.id }
        template.open("externalAppView", "admin-external-app-detail")
    }

	$scope.getPreviewContent = function(app){
        if(!app || !app.data)
            return
		var path = app.data.address
		if(app.data.target === 'adapter'){
			path = '/adapter#' + path
		}
		return $sce.trustAsResourceUrl('/appregistry/app-preview?displayName=' + lang.translate(app.data.displayName) + '&icon=' + encodeURIComponent(app.data.icon) + '&target=' + app.data.target + '&path=' + encodeURIComponent(path))
	}

	$scope.inherited = function(app){
		return app && app.data.structureId !== $scope.school.id
	}

	$scope.scopeable = function(app){
		return app && (
            $scope.isCentralAdmin() ||
		    _.chain(model.me.functions).filter(function(item){return item.code === "ADMIN_LOCAL"}).pluck("scope").flatten().value().indexOf(app.data.structureId) > -1
        )
	}

	$scope.lockExternalApp = function(app){
		app.lock().done(function(data){
			app.data.locked = data.locked
			$scope.$apply()
		})
	}

    $scope.isLinked = function(group, app){
        if(!group || !app)
            return false
        var roleId = app.roles[0].role.id
        return group.roles.indexOf(roleId) >= 0
    }

    $scope.getLinkedGroups = function(groups, app){
        var roleId = app.roles[0].role.id
        return groups.filter(function(group){
            return group.roles.indexOf(roleId) >= 0
        })
    }
    $scope.getUnlinkedGroups = function(groups, app){
        var roleId = app.roles[0].role.id
        return groups.filter(function(group){
            return group.roles.indexOf(roleId) < 0
        })
    }

    $scope.linkedGroupsOpts = {
        showLinked: false,
        orderLinked: false,
        filterLinked: function(app){
            if(!this.showLinked)
                return function(){ return true }
            return function(group){
                return $scope.isLinked(group, app)
            }
        },
        orderByLinked: function(app){
            if(!this.orderLinked)
                return function(group){
                    var score = 3
                    if(group._order){
                        score += group._order.structGroup ? -1 : 0
                        score += group._order.linked ? -2 : 0
                    }
                    return score
                }
            return function(group){
                return $scope.isLinked(group, app) ? 0 : 1
            }
        },
        reorderGroups: function(){
            $scope.school.groups.all.forEach(function(group){
                if(!group)
                    return
                group._order = {
                    linked: $scope.isLinked(group, $scope.externalApp),
                    structGroup: group.name.indexOf($scope.school.name) > -1
                }
            })
        }
    }

    $scope.switchExternalAppGroupLink = function(group, app){
        if(!app || !group || app.data.locked)
            return
        if($scope.isLinked(group, app)){
            var idx = group.roles.indexOf(app.roles[0].role.id)
            group.roles.splice(idx, 1)
            group.removeLink(app.roles[0].role.id).error(function(){
                group.roles.push(app.roles[0].role.id)
                notify.error('appregistry.notify.attribution.error')
                $scope.$apply()
            })
        } else {
            group.roles.push(app.roles[0].role.id)
            group.addLink(app.roles[0].role.id).error(function(){
                group.roles.splice(group.roles.indexOf(app.roles[0].role.id), 1)
                notify.error('appregistry.notify.attribution.error')
                $scope.$apply()
            })
        }
    }

    $scope.createExternalApp = function(app){
        app.save($scope.school.id).done(function(){
            notify.message('success', lang.translate('appregistry.notify.createApp'));
            $scope.school.syncExternalApps($scope.$apply);
            $scope.showExternalAppList();
        }).e400(function(e){
            notify.error(e.responseJSON.error);
        });
    }

    $scope.updateExternalApp = function(app){
        app.save().done(function(){
            notify.info('appregistry.notify.modified');
        }).e400(function(e){
            notify.error(e.responseJSON.error);
        })
    }

    $scope.deleteExternalApp = function(app){
        var action = function(){
            app.delete().done(function(){
                $scope.school.syncExternalApps($scope.$apply)
                $scope.showExternalAppList()
            })
        }
        $scope.notifyTop(lang.translate('appregistry.confirm.app.deletion') + ' ' + $scope.colourText(app.data.displayName) + '.', action)
    }

    $scope.massLinkExternalApp = function(app, profiles){
        var request = app.massAuthorize(_.map(profiles, function(p){ return p.name }))
        request.done(function(){
            notify.info('appregistry.mass.link.notify.ok')
        }).error(function(){
            notify.error('appregistry.mass.link.notify.ko')
        })
        return request
    }
    $scope.massUnlinkExternalApp = function(app, profiles){
        var request = app.massUnauthorize(_.map(profiles, function(p){ return p.name }))
        request.done(function(){
            notify.info('appregistry.mass.unlink.notify.ok')
        }).error(function(){
            notify.error('appregistry.mass.unlink.notify.ko')
        })
        return request
    }

    $scope.multipleCombo = {
        profiles: [
            {
                name: "Teacher",
                translatedName: lang.translate("Teacher"),
                toString: function(){ return this.translatedName }
            },
            {
                name: "Student",
                translatedName: lang.translate("Student"),
                toString: function(){ return this.translatedName }
            },
            {
                name: "Relative",
                translatedName: lang.translate("Relative"),
                toString: function(){ return this.translatedName }
            },
            {
                name: "Personnel",
                translatedName: lang.translate("Personnel"),
                toString: function(){ return this.translatedName }
            },
            {
                name: "Guest",
                translatedName: lang.translate("Guest"),
                toString: function(){ return this.translatedName }
            }
        ],
        selected: {
            profiles: [],
            structure: null
        },
        reset: function(){
            for(var prop in this.selected){
                if(this.selected instanceof Array)
                    this.selected[prop] = []
                else
                    this.selected[prop] = null
            }
        },
        removeElement: function(elt, type){
            this.selected[type].splice(this.selected[type].indexOf(elt), 1)
        },
        comboLabels: {
    		options: lang.translate('options'),
    		searchPlaceholder: lang.translate('search'),
    		selectAll: lang.translate('select.all'),
    		deselectAll: lang.translate('deselect.all')
    	}
    }

    /////// WIDGETS ///////
    $scope.viewWidget = function(widget){
        $scope.widget = widget
        if($scope.school){
            $scope.getWidget($scope.school.id)
        }
    }
    $scope.getWidget = function(structureId){
        $scope.widget.get(structureId, function(){
            $scope.linkedWidgetGroupsOpts.reorderGroups()
            $scope.refreshPreview()
            $scope.$apply()
        })
    }
    $scope.lockWidget = function(widget){
		widget.lock().done(function(data){
			widget.locked = data.locked
			$scope.$apply()
		})
	}

    $scope.isLinkedWidget = function(group, widget){
        return widget.infos && _.some(widget.infos.groups, function(g){ return g.id === group.id })
    }

    $scope.linkedWidgetGroupsOpts = {
        showLinked: false,
        filterLinked: function(widget){
            if(!this.showLinked)
                return function(){ return true }
            return function(group){
                return $scope.isLinkedWidget(group, widget)
            }
        },
        orderByLinked: function(widget){
            return function(group){
                var score = 3
                if(group._order){
                    score += group._order.structGroup ? -1 : 0
                    score += group._order.linkedWidget ? -2 : 0
                }
                return score
            }
        },
        reorderGroups: function(){
            $scope.school.groups.all.forEach(function(group){
                if(!group)
                    return
                group._order = {
                    linkedWidget: $scope.isLinkedWidget(group, $scope.widget),
                    structGroup: group.name.indexOf($scope.school.name) > -1
                }
            })
        }
    }

    $scope.switchWidgetGroupLink = function(group, widget){
        if(!widget || !widget.infos || !group)
            return

        if($scope.isLinkedWidget(group, widget)){
            var backup = _.findWhere(widget.infos.groups, {id: group.id})
            widget.infos.groups.splice(widget.infos.groups.indexOf(backup), 1)
            widget.unlinkWidget(group.id)
                .done(function(){ $scope.$apply() })
                .error(function(){
                    widget.infos.groups.push(backup)
                    notify.error('appregistry.notify.attribution.error')
                    $scope.$apply()
                })
        } else {
            newElt = {id: group.id, mandatory: false}
            widget.infos.groups.push(newElt)
            widget.linkWidget(group.id)
                .done(function(){ $scope.$apply() })
                .error(function(){
                    widget.infos.groups.splice(widget.infos.groups.indexOf(newElt), 1)
                    notify.error('appregistry.notify.attribution.error')
                    $scope.$apply()
                })
        }
    }

    $scope.isWidgetLinkLocked = function(group, widget){
        var matchingGroup = widget.infos && _.findWhere(widget.infos.groups, {id: group.id})
        return matchingGroup ? matchingGroup.mandatory : false
    }

    $scope.lockWidgetGroupLink = function(group, widget){
        var matchingGroup = _.findWhere(widget.infos.groups, {id: group.id})
        if(!matchingGroup)
            return
        var request
        if(!matchingGroup.mandatory){
            request = widget.setMandatory(group.id).done(function(){
                matchingGroup.mandatory = true
            })
        } else {
            request = widget.removeMandatory(group.id).done(function(){
                matchingGroup.mandatory = false
            })
        }
        return request
    }

    $scope.massLinkWidget = function(widget, structure, profiles){
        var request = widget.massAuthorize(structure[0].id, _.map(profiles, function(p){ return p.name }))
        request.done(function(){
            notify.info('appregistry.mass.link.notify.ok')
        }).error(function(){
            notify.error('appregistry.mass.link.notify.ko')
        })
        return request
    }
    $scope.massUnlinkWidget = function(widget, structure, profiles){
        var request = widget.massUnauthorize(structure[0].id, _.map(profiles, function(p){ return p.name }))
        request.done(function(){
            notify.info('appregistry.mass.unlink.notify.ok')
        }).error(function(){
            notify.error('appregistry.mass.unlink.notify.ko')
        })
        return request
    }
    $scope.massSetMandatoryWidget = function(widget, structure, profiles){
        var request = widget.massSetMandatory(structure[0].id, _.map(profiles, function(p){ return p.name }))
        request.done(function(){
            notify.info('appregistry.notify.ok')
        }).error(function(){
            notify.error('appregistry.notify.ko')
        })
        return request
    }
    $scope.massRemoveMandatoryWidget = function(widget, structure, profiles){
        var request = widget.massRemoveMandatory(structure[0].id, _.map(profiles, function(p){ return p.name }))
        request.done(function(){
            notify.info('appregistry.notify.ok')
        }).error(function(){
            notify.error('appregistry.notify.ko')
        })
        return request
    }

    $scope.getWidgetPreviewUrl = function(){
        return $sce.trustAsResourceUrl('/appregistry/widget-preview?widget=' + encodeURIComponent($scope.widget.name))
    }

	/////// ROLES ///////
	$scope.roleMode = 0

	$scope.setCrossRoles = function(roles){
		$scope.crossRoles = roles
	}

	$scope.newRole = function(placeholder_name){
		$scope.role = new Role()
		$scope.role.appRoles = []
		$scope.role.name = placeholder_name ? placeholder_name : ""
	};

	$scope.createRole = function(prefix){
		$scope.role.name = prefix ? prefix + " - " + $scope.role.name : $scope.role.name
		$scope.role.save(function(){
			$scope.roles.syncRoles(function(){
				$scope.crossRoles = $scope.flagCrossRoles()
			})
		})
		$scope.role = undefined
	}

	$scope.deleteRole = function(role){
		//Deletion process
		var deletion = function(){
			role.delete(function(){
				$scope.roles.syncRoles(function(){
					$scope.crossRoles = $scope.flagCrossRoles()
				})
			})
			$scope.role = undefined
		}

		//If we are trying to delete an application role
		if(!_.contains($scope.crossRoles, role)){
			//1st step, deleting the application role from every cross role containing it and storing the cross role in a stack.
			var crossRoleStack = []
			$scope.crossRoles.forEach(function(crossRole){
				var index = crossRole.appRoles.indexOf(role)
				if(index >= 0){
					crossRole.appRoles.splice(index, 1)
					crossRoleStack.push(crossRole)
				}
			})
			//2nd step, saving every cross role we modified before (i.e. contained in the stack).
			//As the calls are asynchronous, we need to be sure that the deletion of the app role takes place only after the final save.
			var launcher = {
				count: crossRoleStack.length + 1,
				decrement: function(){ if(--this.count === 0){ this.launch() } },
				launch: deletion
			}
			launcher.decrement()
			_.forEach(crossRoleStack, function(crossRole){
				crossRole.saveCross(function(){ launcher.decrement() }, true)
			})
		} else {
			deletion()
		}
	}

	$scope.saveAppRole = function(role){
		var crossRoleStack = []
		$scope.crossRoles.forEach(function(crossRole){
			var index = crossRole.appRoles.indexOf(role)
			if(index >= 0){
				crossRoleStack.push(crossRole)
			}
		})

		//Synchronize cross role saving to prevent neo4j deadlocks ...
		var launcher = {
			index: 0,
			action: function(){
				if(crossRoleStack.length > 0 && launcher.index < crossRoleStack.length){
					crossRoleStack[launcher.index].saveCross(function(){
						launcher.index++
						launcher.action()
					}, true)
				} else {
					launcher.terminate()
				}
			},
			terminate: function(){ role.save() }
		}
		launcher.action()
	}

	$scope.saveCrossRole = function(role){
		role.saveCross(function(){
			$scope.roles.syncRoles(function(){
				$scope.crossRoles = $scope.flagCrossRoles()
				$scope.role = $scope.roles.findWhere({name: role.name})
			})
		})
	}

	$scope.hideRoleCreationPanel = function(){
		$scope.role = undefined
	}

	$scope.selectRole = function(role){
		role.selected = !role.selected;
	};

	$scope.viewRole = function(role){
		$scope.role = role;
	}

	$scope.selectAllActions = function(role){
		role = role ? role : $scope.role
		role.actions.all = []
		$scope.application.actions.forEach(function(action){ role.actions.push(action) })
	}

	$scope.deselectAllActions = function(role){
		role = role ? role : $scope.role
		role.actions.all = []
	}

	$scope.crossRoleContains = function(approle){
		return _.contains($scope.role.appRoles, approle)
	}

	$scope.flagCrossRoles = function(){
		return $scope.roles.crossAppRoles($scope.applications).map(function(role){
			role.appRoles = []
			$scope.applications.forEach(function(app){
				var approles = $scope.roles.applicationRolesExclusive(app)
				_.forEach(approles, function(approle){
					if(role.crossRoleContains(approle)){
						role.appRoles.push(approle)
					}
				})
			})
			return role
		})
	}

	$scope.validateCrossRole = function(crossRole){
		if(crossRole.appRoles.length > 0){
			var applicationReference = $scope.applications.find(function(application){
				return application.actions.findWhere({name: crossRole.appRoles[0].actions.all[0].name})
			})
			var differentAppsCheck = _.every(crossRole.appRoles, function(role){
				return role.actions.every(function(action){
					return applicationReference.actions.findWhere({name: action.name})
				})
			})
			return !differentAppsCheck && crossRole.name
		}
		return false
	}

	/////// GROUPS ///////

	$scope.SCROLL_INCREMENT = 100
	$scope.groupsLimit = $scope.SCROLL_INCREMENT
	$scope.incrementGroupsLimit = function(){
		$scope.groupsLimit += $scope.SCROLL_INCREMENT
	}

	$scope.groupContains = function(group, role){
		return _.find(group.roles, function(role_id){
			return role_id === role.id
		})
	}

	$scope.switchGroupRole = function(group, role){
		if($scope.groupContains(group, role)){
			group.roles = _.reject(group.roles, function(r){ return r === role.id })
		} else {
			group.roles.push(role.id)
		}
		group.link()
	}

	$scope.selectAllGroupRoles = function(group){
		var roles = []
		_.forEach($scope.applications.all, function(application){
			roles = _.union(roles, $scope.roles.applicationRolesExclusive(application))
		})
		_.forEach(roles, function(role){
			if(!$scope.groupContains(group, role)){
				group.roles.push(role.id)
			}
		})
		group.link()
	}

	$scope.deselectAllGroupRoles = function(group){
		var roles = []
		_.forEach($scope.applications.all, function(application){
			roles = _.union(roles, $scope.roles.applicationRolesExclusive(application))
		})
		_.forEach(roles, function(role){
			group.roles = _.reject(group.roles, function(r){ return r === role.id })
		})
		group.link()
	}

	// INIT //
	if($scope.isCentralAdmin()){
		model.roles.syncRoles()
		model.applications.syncApps()
	}

}
