function HistoryAdminController($scope, model, date) {

    $scope.lang = lang
    $scope.structures = model.structures
    $scope.pending = model.pendingNotifications
    $scope.treated = model.treatedNotifications
    $scope.pending.on('sync', function(){
        $scope.$apply()
    })
    $scope.treated.on('sync', function(){
        $scope.$apply()
    })

    /* THEMES */

    $scope.themes = [{
        name: "pink",
        path: "default"
    }, {
        name: "orange",
        path: "orange"
    }, {
        name: "blue",
        path: "blue"
    }, {
        name: "purple",
        path: "purple"
    }, {
        name: "red",
        path: "red"
    }, {
        name: "green",
        path: "green"
    }, {
        name: "grey",
        path: "grey"
    }]
    $scope.setTheme = function(theme) {
        ui.setStyle('/public/admin/' + theme.path + '/')
        http().putJson('/userbook/preference/admin', {
            name: theme.name,
            path: theme.path
        })
    }

    /* TOP NOTIFICATIONS */

    $scope.topNotification = {
        show: false,
        message: "",
        confirm: null,
        additional: [{
            label: lang.translate('timeline.admin.delete'),
            action: function(){}
        }],
        labels: {
            confirm: lang.translate('timeline.admin.keep'),
            cancel: lang.translate('cancel'),
            ok: lang.translate('ok')
        }
    }
    $scope.notifyTop = function(text, action, addaction){
        $scope.topNotification.message = "<p>"+text+"</p>"
        $scope.topNotification.confirm = action
        $scope.topNotification.additional[0].action = function(){
            addaction()
            $scope.topNotification.show = false
        }
        $scope.topNotification.show = true
    }
    $scope.notifyTreatment = function(notification) {
        var notificationStructureId = $scope.structure.id

        var keepNotification = function(){
            notification.action(notificationStructureId, 'keep').done(function(){
                if(notification.reportAction) {
                    notification.reportAction.action = "KEEP"
                } else {
                    $scope.pending.remove(notification)
                }
                $scope.$apply()
            }.bind(this))
        }
        var deleteNotification = function(){
            notification.action(notificationStructureId, 'delete').done(function(){
                if(notification.reportAction) {
                    notification.reportAction.action = "DELETE"
                } else {
                    $scope.pending.remove(notification)
                }
                $scope.$apply()
            }.bind(this))
        }
        $scope.notifyTop(
            lang.translate('timeline.admin.action.text'),
            keepNotification,
            deleteNotification)
    }

    /* STRUCTURE PICKER */

    $scope.viewStructure = function(structure){
		$scope.$parent.structure = structure
        $scope.pending.reset()
        $scope.treated.reset()
        $scope.pending.feed(structure.id)
        $scope.treated.feed(structure.id)
	}

    $scope.filterTopStructures = function(structure){
		return !structure.parents
	}

	$scope.selectOnly = function(structure, structureList){
		_.forEach(structure.children, function(s){ s.selected = false })
		_.forEach(structureList, function(s){ s.selected = s.id === structure.id ? true : false })
	}

    /* DATE FORMAT */

    $scope.formatDate = function(dateString){
		return date.calendar(dateString).toLowerCase()
	}

    /* TABS */

    $scope.tabs = {
        selected: '',
        select: function(tabName) {
            this.selected = tabName
            $scope.currentModel = $scope[tabName]
        }
    }

}
