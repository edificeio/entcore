function AdminDirectoryController($scope) {
    template.open('main', 'admin/welcome-message');

    $scope.messages = model.messages;
    $scope.edited = model.edited;

    $scope.messages.on('sync', function () {
        $scope.edited.message = $scope.messages.findWhere({ lang: currentLanguage }) || $scope.editedMessage;
        setTimeout(function () {
            model.messages.display = true;
            $scope.$apply();
        }, 500);
    });

    $scope.saveChanges = function () {
        model.messages.save();
    }
}