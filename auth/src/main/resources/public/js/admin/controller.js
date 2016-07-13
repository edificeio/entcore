function AdminDirectoryController($scope) {
    template.open('main', 'admin/welcome-message');
    $scope.message = model.message;
    $scope.message.sync();

    $scope.saveChanges = function () {
        this.message.save();
    }
}