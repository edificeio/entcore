function TimetableController($scope, $rootScope, model, template, route, date, lang) {
	$scope.template = template;
	$scope.lang = lang;
	$scope.identity = angular.identity;
	$scope.structures =  model.structures;

    $scope.viewStructure = function(structure){
        $scope.structure = structure;
        $scope.savedType = structure.timetable;
        $scope.importFile = "";
        $scope.errors = {};
        $scope.getClassesMapping(structure);
    };

    $scope.getClassesMapping = function(structure) {
        structure.classesMapping(function(data) {
            if (data && data.classNames) {
                data.classNames.sort();
            }
            if (data && !data.mapping) {
                data.mapping = {};
            }
            $scope.cm = data;
            $scope.$apply();
        });
    };

    $scope.filterTopStructures = function(structure){
        return !structure.parents;
    };

    $scope.selectOnly = function(structure, structureList){
        _.forEach(structure.children, function(s){ s.selected = false })
        _.forEach(structureList, function(s){ s.selected = s.id === structure.id ? true : false })
    };

    $scope.updateType = function(structure) {
        var action = function() {
            var t = structure.timetable;
            structure.init(function () {
                $scope.savedType = t;
                notify.info('directory.params.success');
                $scope.$apply();
            });
        };
        $scope.notifyTop(lang.translate('timetable.confirm.change.type'), action);
    };

    $scope.updateClassesMapping = function(structure, cm) {
        structure.updateClassesMapping(cm);
    };

    $scope.import = function(structure, importFile) {
        $scope.errors = {};
        $scope.importing = true;
        var formData = new FormData();
        formData.append("file", importFile[0]);
        structure.import(formData, function(data) {
            $scope.getClassesMapping(structure);
            if (data.error || data.errors) {
                $scope.displayErrors(data);
                $scope.$apply();
            } else {
                notify.info('directory.params.success');
            }
            $scope.importing = false;
        });
    };

    $scope.displayErrors = function(data) {
        if (data.error) {
            $scope.errors = {};
            $scope.errors['error.global'] = [];
            $scope.errors['error.global'].push(lang.translate(data.error));
        } else {
            $scope.errors = data.errors;
        }
        $scope.errors = _.map($scope.errors, function (errors, file) {
            return {"title" : lang.translate(file), "elements" : errors };
        });
    };

    /////// TOP NOTIFICATIONS ///////
    $scope.topNotification = {
        show: false,
        message: "",
        confirm: null
    };
    $scope.notifyTop = function(text, action){
        $scope.topNotification.message = "<p>"+text+"</p>"
        $scope.topNotification.confirm = action
        $scope.topNotification.show = true
    };
    $scope.colourText = function(text){
        return '<span class="colored">'+text+'</span>'
    };

}
