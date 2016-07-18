function AdminDirectoryController($scope) {
    template.open('main', 'admin/welcome-message');
    $scope.message = model.message;
    $scope.message.sync(function () {
        $scope.message.display = true;
        $scope.$apply();
    });

    $scope.saveChanges = function () {
        this.message.save();
    }
}