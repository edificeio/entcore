export function safeApply($scope: any) {
  const phase = $scope.$root.$$phase;
  if (phase !== "$apply" && phase !== "$digest") {
    $scope.$apply();
  }
}
