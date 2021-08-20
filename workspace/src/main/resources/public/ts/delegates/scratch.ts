export interface ScratchDelegateScope {
    onInit(cab: () => void);
    canBeOpenOnScratch({metadata}): boolean;
    openOnScratch(file,scratch_url): void;
}

export function ScratchDelegate($scope: ScratchDelegateScope, $route) {
    $scope.onInit(() => {
        $scope.canBeOpenOnScratch = ({metadata}) => {
            return ["sb","sb2","sb3"].includes(metadata.extension) && metadata["content-type"] === "application/octet-stream";
        };
        $scope.openOnScratch = (file,scratch_url) => {
            window.open(`${scratch_url}/workspace/document/base64/${file._id}`);
        }
    });
}
