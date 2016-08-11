function AdminDirectoryController($scope) {
    template.open('main', 'admin/welcome-message');
    $scope.message = model.message;
    $scope.message.sync(function () {
        $scope.message.display = true;
        setTimeout(function () {
            $scope.$apply();
        }, 500);
    });

    $scope.saveChanges = function () {
        this.message.save();
    }
}