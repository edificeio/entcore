function TimelineAdminController($scope, model){
    $scope.lang = lang
    $scope.config = model.configs
    $scope.notifs = model.notifications
    $scope.typesMap = {}

    $scope.constants = {
        "defaultFrequency": [{label: "IMMEDIATE"}, {label: "DAILY"}, {label: "WEEKLY"}],
        "restriction": [{label: "NONE"}, {label: "INTERNAL"}, {label: "EXTERNAL"}]
    }

    $scope.saveConfig = function(notif){
        notif.config.upsert()
    }

    model.notifications.one('sync', function(){
        model.notifications.forEach(function(notif){
            if(!$scope.typesMap[notif.type]){
                $scope.typesMap[notif.type] = []
            }
            $scope.typesMap[notif.type].push(notif)
        })
    })

    lang.addBundle('/timeline/i18nNotifications', function(){
        $scope.$apply(function(){
		    $scope.notifications = model.notifications
        })
    })
}
