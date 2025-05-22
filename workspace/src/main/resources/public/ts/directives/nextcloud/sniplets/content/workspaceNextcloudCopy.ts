import { ng } from "entcore";

interface INextcloudCopyController {
  
}

export const nextcloudCopyController = ng.controller(
  "NextcloudCopyController", [
    "$scope",
    ($scope: any) => {
      
    }
  ]
);

export const workspaceNextcloudCopy = ng.directive(
  "workspaceNextcloudCopy",
  () => {
    return {
      restrict: "E",
      scope: {
        treeProps: "=",
        folder: "=",
      },
      controller: "NextcloudCopyController",
    };
  },
);
