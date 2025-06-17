import { ng, routes } from "entcore";
import { workspaceController } from "./controller";
import { cssTransitionEnd } from "./directives/cssTransitions";
import { csvViewer } from "./directives/csvViewer";
import { dropzoneOverlay } from "./directives/dropzoneOverlay";
import { fileViewer } from "./directives/fileViewer";
import { folderPicker2 } from "./directives/folderPicker2";
import { folderTree2, folderTreeInner2 } from "./directives/folderTree2";
import { importFiles } from "./directives/import";
import { lazyLoadImg } from "./directives/lazyLoad";
import {
  workspaceNextcloudContent,
  workspaceNextcloudContentController,
} from "./directives/nextcloud/components/content/contentViewer.component";
import {
  workspaceNextcloudFolder,
  workspaceNextcloudFolderController,
} from "./directives/nextcloud/nextcloudFolder.directive";
import { NextcloudService } from "./directives/nextcloud/services/nextcloud.service";
import { NextcloudEventService } from "./directives/nextcloud/services/nextcloudEvent.service";
import { NextcloudUserService } from "./directives/nextcloud/services/nextcloudUser.service";
import { pdfViewer } from "./directives/pdfViewer";
import { syncDocumentViewer } from "./directives/syncDocumentViewer";
import { txtViewer } from "./directives/txtViewer";

routes.define(function ($routeProvider) {
  $routeProvider
    .when("/", {
      action: "openOwn",
    })
    .when("/folder/:folderId", {
      action: "viewFolder",
    })
    .when("/shared/folder/:folderId", {
      action: "viewSharedFolder",
    })
    .when("/shared", {
      action: "openShared",
    })
    .when("/trash", {
      action: "openTrash",
    })
    .when("/apps", {
      action: "openApps",
    })
    .when("/external", {
      action: "openExternal",
    })
    .otherwise({
      redirectTo: "/",
    });
});

ng.controllers.push(workspaceController);
ng.directives.push(importFiles);
ng.directives.push(fileViewer);
ng.directives.push(syncDocumentViewer);
ng.directives.push(pdfViewer);
ng.directives.push(cssTransitionEnd);
ng.directives.push(dropzoneOverlay);
ng.directives.push(lazyLoadImg);
ng.directives.push(csvViewer);
ng.directives.push(txtViewer);

// Nextcloud
// Services
ng.services.push(NextcloudService);
ng.services.push(NextcloudUserService);
ng.services.push(NextcloudEventService);

// Folder
ng.directives.push(workspaceNextcloudFolder);
ng.controllers.push(workspaceNextcloudFolderController);

// Folder Picker
ng.directives.push(folderTree2);
ng.directives.push(folderTreeInner2);
ng.directives.push(folderPicker2);

// Content
ng.directives.push(workspaceNextcloudContent);
ng.controllers.push(workspaceNextcloudContentController);
