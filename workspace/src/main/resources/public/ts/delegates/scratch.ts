export interface ScratchDelegateScope {
    onInit(cab: () => void);
    canBeOpenOnScratch({metadata}): boolean;
    openOnScratch(file): void;
}

export function ScratchDelegate($scope: ScratchDelegateScope, $route) {
    $scope.onInit(() => {
        $scope.canBeOpenOnScratch = ({metadata}) => {
            return ["sb","sb2","sb3"].includes(metadata.extension) && metadata["content-type"] === "application/octet-stream";
        };
        $scope.openOnScratch = (file) => {
            window.open(`/scratch/open?ent_id=${file._id}`);
        }
    });
}
