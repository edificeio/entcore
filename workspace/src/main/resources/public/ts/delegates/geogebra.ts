
export interface GeogebraDelegateScope {
    onInit(cab: () => void);
    canBeOpenOnGGB({metadata}): boolean;
    openOnGGB(file): void;
}

export function GeogebraDelegate($scope: GeogebraDelegateScope, $route) {
    $scope.onInit(() => {
        $scope.canBeOpenOnGGB = ({metadata}) => {
            return metadata.extension === "ggb";
        };
        $scope.openOnGGB = (file) => {
            window.open(`/geogebra#/${file._id}?fileName=${file.name}.ggb`);
        }
    });
}