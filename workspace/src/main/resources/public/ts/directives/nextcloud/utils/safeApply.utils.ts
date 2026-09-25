export function safeApply($scope: any) {
  // Scope may already be destroyed if an async request resolves after navigating away.
  if (!$scope || !$scope.$root) return;
  let phase = $scope.$root.$$phase;
  if (phase !== "$apply" && phase !== "$digest") {
    $scope.$apply();
  }
}
