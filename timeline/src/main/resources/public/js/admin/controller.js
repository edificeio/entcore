function TimelineAdminController($scope, model){
    $scope.lang = lang
    $scope.config = model.configs
    $scope.notifs = model.notifications
    $scope.typesMap = {}
    $scope.types = []

    $scope.constants = {
        "defaultFrequency": [{label: "NEVER"}, {label: "IMMEDIATE"}, {label: "DAILY"}, {label: "WEEKLY"}],
        "restriction": [{label: "NONE"}, {label: "INTERNAL"}, {label: "EXTERNAL"}]
    }

    $scope.saveConfig = function(notif){
        notif.config.upsert()
    }

    $scope.orderTypes = function(type){
        return lang.translate(type.type.toLowerCase())
    }

    $scope.orderNotifs = function(notif){
        return lang.translate(notif.key.toLowerCase())
    }

    model.notifications.one('sync', function(){
        model.notifications.forEach(function(notif){
            if(!$scope.typesMap[notif.type]){
                $scope.typesMap[notif.type] = []
            }
            $scope.typesMap[notif.type].push(notif)
        })
        for(var type in $scope.typesMap){
            $scope.types.push({
                type: type,
                notifs: $scope.typesMap[type]
            })
        }
    })

    lang.addBundle('/timeline/i18nNotifications', function(){
        $scope.$apply(function(){
		    $scope.notifications = model.notifications
        })
    })
}
