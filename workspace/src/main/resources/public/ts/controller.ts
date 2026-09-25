// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

import { angular, model, idiom as lang, idiom, moment, ng, notify, template } from "entcore";
import {
  NavigationDelegate,
  NavigationDelegateScope,
} from "./delegates/navigation";
import { ActionDelegate, ActionDelegateScope } from "./delegates/actions";
import { TreeDelegate, TreeDelegateScope } from "./delegates/tree";
import { CommentDelegate, CommentDelegateScope } from "./delegates/comments";
import { DragDelegate, DragDelegateScope } from "./delegates/drag";
import { SearchDelegate, SearchDelegateScope } from "./delegates/search";
import { RevisionDelegate, RevisionDelegateScope } from "./delegates/revisions";
import { KeyboardDelegate, KeyboardDelegateScope } from "./delegates/keyboard";
import { LoolDelegate, LoolDelegateScope } from "./delegates/lool";
import {
  CursorUpdate,
  DocumentCursor,
  DocumentCursorParams,
  models,
  workspaceService,
} from "./services";
import { DocumentActionType } from "entcore/types/src/ts/workspace/services";
import {ScratchDelegate, ScratchDelegateScope} from "./delegates/scratch";
import {GeogebraDelegate, GeogebraDelegateScope} from "./delegates/geogebra";
import {GOOGLE_DRIVE_CREATE_DOCUMENT_TYPES} from "./directives/google-drive/models/googleDriveCreateDocumentType.model";

const NEXTCLOUD_VIEW_RIGHT = 'fr.openent.nextcloud.controller.NextcloudController|view';

declare var ENABLE_LOOL: boolean;
declare var ENABLE_SCRATCH: boolean;
declare var ENABLE_NEXTCLOUD: boolean;
declare var USE_NEXTCLOUD_SNIPLET: boolean;
declare var ENABLE_GOOGLE_DRIVE: boolean;
declare var ENABLE_GGB: boolean;
declare var DISABLE_FULL_TEXT_SEARCH: boolean;
export interface WorkspaceScope extends RevisionDelegateScope, NavigationDelegateScope, TreeDelegateScope, ActionDelegateScope, CommentDelegateScope, DragDelegateScope, SearchDelegateScope, KeyboardDelegateScope, LoolDelegateScope, ScratchDelegateScope, GeogebraDelegateScope {
	ENABLE_LOOL: boolean;
	ENABLE_SCRATCH: boolean;
	ENABLE_GGB: boolean;
	ENABLE_NEXTCLOUD: boolean;
	HAS_NEXTCLOUD_RIGHT: boolean;
	USE_NEXTCLOUD_SNIPLET: boolean;
	ENABLE_GOOGLE_DRIVE: boolean;
	DISABLE_FULL_TEXT_SEARCH: boolean;
	isGoogleDriveImportableFolder(): boolean;
	triggerGoogleDriveImport(): void;
	isGoogleDriveTrashbinOpen(): boolean;
	isGoogleDriveTrashEmptyable(): boolean;
	triggerGoogleDriveCreateFolder(): void;
	triggerGoogleDriveEmptyTrash(): void;
	googleDriveDocTypes: { label: string, icon: string, type: string }[];
	isGoogleDriveCreateDocumentMenuOpen: boolean;
	toggleGoogleDriveCreateDocumentMenu(): void;
	triggerGoogleDriveCreateDocument(docType: { type: string }): void;
	isNextcloudImportableFolder(): boolean;
	triggerNextcloudImport(): void;
	isNextcloudTrashbinOpen(): boolean;
	isNextcloudTrashEmptyable(): boolean;
	triggerNextcloudCreateFolder(): void;
	triggerNextcloudEmptyTrash(): void;
	getNextcloudTreeScope(): any;
	footerBackgroundColor: string;
	footerLeft: number;
	footerWidth: number;
	contentAreaHeight: number;
	recomputeWorkspaceFooterOffsets(): void;
	documentList:models.DocumentsListModel;
	documentListSorted:models.DocumentsListModel;
	//new
	lightboxDelegateClose: () => boolean
	newFile: { chosenFiles: any[] }
	//
	display: { nbFiles: number, importFiles?: boolean, viewFile?: models.Element, share?: boolean, loolModal?: boolean }
	lastRoute: string
	safeApply(a?);
	//help
	getHelpForFolder(folder: models.Element): string
	//
	setLightboxDelegateClose(f: () => boolean)
	resetLightboxDelegateClose()
	//
	showCarousel(): boolean
	formatDocumentSize(size: number): string
	shortDate(el: string | number): string
	longDate(date: string): number
	translate(key: string): string
	cancelRequest(file)
	isUploadedImage(): boolean
	createDocumentCursor(onUpdate: CursorUpdate, params: DocumentCursorParams): void
	//selection
	//per-tile "..." menu (icon view)
	openTileMenuFor: models.Element | null;
	isTileMenuOpen(item: models.Element): boolean;
	toggleTileMenu(item: models.Element): void;
	isTileMenuActionVisible(item: models.Element, action: "download" | "rename" | "share" | "move" | "copy" | "trash" | "restore" | "delete"): boolean;
	onTileOpen(item: models.Element): void;
	onTileDownload(item: models.Element): void;
	onTileRename(item: models.Element): void;
	onTileShare(item: models.Element): void;
	onTileMove(item: models.Element): void;
	onTileCopy(item: models.Element): void;
	onTileTrash(item: models.Element): void;
	onTileRestore(item: models.Element): void;
	onTileDeletePermanently(item: models.Element): void;
}
export let workspaceController = ng.controller('Workspace', ['$scope', '$rootScope', '$timeout', '$location', '$anchorScroll', 'route', '$route','$filter', ($scope: WorkspaceScope, $rootScope, $timeout, $location, $anchorScroll, route, $route, $filter) => {
	let _currentCursor: DocumentCursor = null;
	$scope.lightboxDelegateClose = () => false;
	$scope.setLightboxDelegateClose = function (f) {
		$scope.lightboxDelegateClose = f;
	}
	$scope.resetLightboxDelegateClose = function () {
		$scope.lightboxDelegateClose = () => false;

	}
	const nextCursor = async () => {
		if (_currentCursor != null) {
			await _currentCursor.next();
		}
	}
	$scope.createDocumentCursor = function (onUpdate, params) {
		_currentCursor = new DocumentCursor(params, onUpdate);
		nextCursor();
	}
	let displayNotFoundErrorLastId = null;
	const displayNotFoundError = function (folderId) {
		if (folderId == displayNotFoundErrorLastId) {
			return;
		}
		//avoid display message twice if we have not reload page
		notify.error(idiom.translate("workspace.element.uri.notfound"));
		displayNotFoundErrorLastId = folderId;
	}
	/**
	 * Routes
	 */
	route({
		viewFolder: function (params) {
			$scope.lastRoute = window.location.href
			//attend chargement arbo dossier
			$scope.onTreeInit(async () => {
				const success = await $scope.openFolderById(params.folderId)
				!success && displayNotFoundError(params.folderId);
			})
		},
		viewSharedFolder: function (params) {
			$scope.lastRoute = window.location.href;
			//attend chargement arbo dossier
			$scope.onTreeInit(async () => {
				const success = $scope.openFolderById(params.folderId)
				!success && displayNotFoundError(params.folderId);
			})
		},
		openShared: function (params) {
			$scope.lastRoute = window.location.href;
			$scope.onTreeInit(() => {
				$scope.setCurrentTree("shared")
			})
		},
		openOwn: function (params) {
			$scope.lastRoute = window.location.href;
			$scope.onTreeInit(() => {
				$scope.setCurrentTree("owner")
			})
		},
		openExternal: function (params) {
			$scope.lastRoute = window.location.href;
			$scope.onTreeInit(() => {
				$scope.setCurrentTree("external")
			})
		},
		openTrash: function (params) {
			$scope.lastRoute = window.location.href;
			$scope.onTreeInit(() => {
				$scope.setCurrentTree("trash")
			})
		},
		openApps: function (params) {
			$scope.lastRoute = window.location.href;
			$scope.onTreeInit(() => {
				$scope.setCurrentTree("protected")
			})
		},
		openLoolModal: function (params) {
			$scope.openLoolModal();
		}
	});
	//
	const inits: (() => void)[] = [];
	$scope.onInit = function (cb) {
		inits.push(cb)
	}
	/**
	 * Delegates
	 */
	NavigationDelegate($scope, $location, $anchorScroll, $timeout);
	TreeDelegate($scope, $location);
	ActionDelegate($scope);
	CommentDelegate($scope);
	DragDelegate($scope);
	SearchDelegate($scope);
	RevisionDelegate($scope);
	KeyboardDelegate($scope);
	ENABLE_LOOL && LoolDelegate($scope, $route, $location);
	ENABLE_SCRATCH && ScratchDelegate($scope, $route);
	ENABLE_GGB && GeogebraDelegate($scope, $route);
	$scope.ENABLE_LOOL = ENABLE_LOOL;
	$scope.ENABLE_SCRATCH = ENABLE_SCRATCH;
	$scope.ENABLE_GGB = ENABLE_GGB;
	$scope.ENABLE_NEXTCLOUD = ENABLE_NEXTCLOUD;
	$scope.HAS_NEXTCLOUD_RIGHT = model.me.hasWorkflow(NEXTCLOUD_VIEW_RIGHT);
	$scope.USE_NEXTCLOUD_SNIPLET = USE_NEXTCLOUD_SNIPLET;
	$scope.ENABLE_GOOGLE_DRIVE = ENABLE_GOOGLE_DRIVE;
	$scope.DISABLE_FULL_TEXT_SEARCH = DISABLE_FULL_TEXT_SEARCH;

	// Per-tile "..." menu (icon view) — acts on the tile's own item without going through the real
	// selection state, so opening it never visually selects the tile. Actions here reuse the same
	// delegate functions the real selection toolbar uses, briefly setting item.selected so those
	// functions (which read the current selection) pick up the right item, then unsetting it right
	// after for the actions that read selection synchronously with no confirm dialog. Trash/delete
	// actions open a confirm dialog that reads selection later, so selected is left as-is there —
	// same as the real toolbar flow, which is the whole point.
	$scope.openTileMenuFor = null;
	$scope.isTileMenuOpen = function (item: models.Element): boolean {
		return $scope.openTileMenuFor === item;
	};
	$scope.toggleTileMenu = function (item: models.Element): void {
		$scope.openTileMenuFor = $scope.isTileMenuOpen(item) ? null : item;
	};
	document.addEventListener("click", function (event: MouseEvent): void {
		if (!$scope.openTileMenuFor) return;
		if ((event.target as HTMLElement)?.closest(".tile-menu-wrapper")) return;
		$scope.openTileMenuFor = null;
		$scope.safeApply();
	});
	const withSelected = (item: models.Element, action: () => void): void => {
		item.selected = true;
		action();
		item.selected = false;
		$scope.safeApply();
	};
	const withSelectedPersisted = (item: models.Element, action: () => void): void => {
		item.selected = true;
		action();
		$scope.safeApply();
	};
	$scope.isTileMenuActionVisible = function (item: models.Element, action): boolean {
		const filter = $scope.currentTree?.filter;
		const isFolder = workspaceService.isFolder(item);
		switch (action) {
			case "download":
				return filter !== "external" && filter !== "trash" && workspaceService.isActionAvailable("download", [item]);
			case "rename":
				return filter !== "trash" && (!isFolder || filter !== "external")
					&& !!item.myRights && !!item.myRights["manager"];
			case "share":
				return (filter === "shared" || filter === "owner")
					&& workspaceService.isActionAvailable("share", [item]);
			case "move":
				return (filter === "owner" || filter === "shared")
					&& workspaceService.isActionAvailable("move", [item]);
			case "copy":
				return (filter === "owner" || filter === "shared" || filter === "protected")
					&& workspaceService.isActionAvailable("copy", [item]);
			case "trash":
				if (filter === "trash") return false;
				if (filter === "external") return !isFolder;
				return !!item.myRights && !!item.myRights["manager"];
			case "restore":
			case "delete":
				return filter === "trash" && !!item.myRights && !!item.myRights["manager"];
		}
		return false;
	};
	$scope.onTileOpen = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		if (workspaceService.isFolder(item)) {
			$scope.openFolderRoute(item);
		} else {
			$scope.viewFile(item);
		}
	};
	$scope.onTileDownload = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		workspaceService.downloadFiles([item], $scope.currentTree.filter === "trash");
	};
	$scope.onTileRename = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelected(item, () => $scope.openRenameView(item));
	};
	$scope.onTileShare = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelected(item, () => $scope.openShareView());
	};
	$scope.onTileMove = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelected(item, () => $scope.openMoveView());
	};
	$scope.onTileCopy = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelected(item, () => $scope.openCopyView());
	};
	$scope.onTileTrash = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelectedPersisted(item, () => $scope.toTrashConfirm());
	};
	$scope.onTileRestore = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelected(item, () => $scope.restore());
	};
	$scope.onTileDeletePermanently = function (item: models.Element): void {
		$scope.openTileMenuFor = null;
		withSelectedPersisted(item, () => $scope.deleteConfirm());
	};

	// Header button rendered here, not in the GD component, to stay aligned across every tree; reaches into the GD scopes directly since they have no relation to this one.
	$scope.isGoogleDriveImportableFolder = function (): boolean {
		const gdTreeScope: any = angular.element(document.getElementById("google-drive-folder-tree")).scope();
		return !gdTreeScope?.isTrashbinOpen && !gdTreeScope?.isSharedViewOpen;
	};
	$scope.triggerGoogleDriveImport = function (): void {
		const gdContentScope: any = angular.element(document.getElementById("google-drive-content")).scope();
		gdContentScope?.triggerImportFiles();
	};

	$scope.isGoogleDriveTrashbinOpen = function (): boolean {
		const gdTreeScope: any = angular.element(document.getElementById("google-drive-folder-tree")).scope();
		return !!gdTreeScope?.isTrashbinOpen;
	};
	$scope.isGoogleDriveTrashEmptyable = function (): boolean {
		// Not gdTreeScope.documents — that's the stable tree-root structure, always non-empty; the real trash contents live on the content scope.
		const gdContentScope: any = angular.element(document.getElementById("google-drive-content")).scope();
		return (gdContentScope?.documents?.length ?? 0) > 0;
	};
	$scope.triggerGoogleDriveCreateFolder = function (): void {
		const gdTreeScope: any = angular.element(document.getElementById("google-drive-folder-tree")).scope();
		gdTreeScope?.folderCreation?.toggleCreateFolder(true, null);
	};
	$scope.triggerGoogleDriveEmptyTrash = function (): void {
		const gdTreeScope: any = angular.element(document.getElementById("google-drive-folder-tree")).scope();
		if (gdTreeScope?.emptyTrashbin) gdTreeScope.emptyTrashbin.lightbox.emptyTrash = true;
	};

	// The create URL is built server-side (GET /googledrive/files/create/:type, 302-redirect); entries only carry the doc type.
	$scope.googleDriveDocTypes = GOOGLE_DRIVE_CREATE_DOCUMENT_TYPES;
	$scope.isGoogleDriveCreateDocumentMenuOpen = false;
	$scope.toggleGoogleDriveCreateDocumentMenu = function (): void {
		$scope.isGoogleDriveCreateDocumentMenuOpen = !$scope.isGoogleDriveCreateDocumentMenuOpen;
	};
	$scope.triggerGoogleDriveCreateDocument = function (docType: { type: string }): void {
		$scope.isGoogleDriveCreateDocumentMenuOpen = false;
		window.open(`/googledrive/files/create/${docType.type}`, "_blank");
	};
	// Closes the dropdown on outside click; clicks inside the wrapper are left to their own ng-click.
	document.addEventListener("click", function (event: MouseEvent): void {
		if (!$scope.isGoogleDriveCreateDocumentMenuOpen) return;
		if ((event.target as HTMLElement)?.closest(".google-drive-create-document-wrapper")) return;
		$scope.isGoogleDriveCreateDocumentMenuOpen = false;
		$scope.safeApply();
	});

	// Mirrors the Google Drive block above; Nextcloud has no shared-folder equivalent.
	$scope.isNextcloudImportableFolder = function (): boolean {
		const ncTreeScope: any = angular.element(document.getElementById("nextcloud-folder-tree")).scope();
		return !ncTreeScope?.isTrashbinOpen;
	};
	$scope.triggerNextcloudImport = function (): void {
		const ncContentScope: any = angular.element(document.getElementById("nextcloud-content")).scope();
		ncContentScope?.upload?.startImportFlow();
	};
	$scope.isNextcloudTrashbinOpen = function (): boolean {
		const ncTreeScope: any = angular.element(document.getElementById("nextcloud-folder-tree")).scope();
		return !!ncTreeScope?.isTrashbinOpen;
	};
	$scope.isNextcloudTrashEmptyable = function (): boolean {
		// See isGoogleDriveTrashEmptyable — same reasoning.
		const ncContentScope: any = angular.element(document.getElementById("nextcloud-content")).scope();
		return (ncContentScope?.documents?.length ?? 0) > 0;
	};
	$scope.triggerNextcloudCreateFolder = function (): void {
		const ncTreeScope: any = angular.element(document.getElementById("nextcloud-folder-tree")).scope();
		ncTreeScope?.folderCreation?.toggleCreateFolder(true, null);
	};
	// Lives here, not in workspace-nextcloud-folder.html, so it's a direct flex child of nav.vertical and its CSS "order: 999" actually applies.
	$scope.getNextcloudTreeScope = function (): any {
		return angular.element(document.getElementById("nextcloud-folder-tree")).scope();
	};
	$scope.triggerNextcloudEmptyTrash = function (): void {
		const ncTreeScope: any = angular.element(document.getElementById("nextcloud-folder-tree")).scope();
		if (ncTreeScope?.emptyTrashbin) ncTreeScope.emptyTrashbin.lightbox.emptyTrash = true;
	};

	// Classic view's height doesn't reliably reach the viewport bottom, so the footer's box has to be measured rather than reserved with padding.
	function getWorkspaceFooterBackgroundColor(): string {
		let el: Element | null = document.querySelector("nav.vertical.nav-droppable.mobile-navigation");
		while (el) {
			const bg = window.getComputedStyle(el).backgroundColor;
			if (bg && bg !== "rgba(0, 0, 0, 0)" && bg !== "transparent") {
				return bg;
			}
			el = el.parentElement;
		}
		return "transparent";
	}
	function getWorkspaceContentBox(): Element | null {
		return document.querySelector(".list-view, .icons-view");
	}
	// Sidebar isn't guaranteed flush with the viewport edge, so left/width are measured rather than hardcoded to avoid the footer spilling into the document list.
	function getWorkspaceSidebarRect(): { left: number; width: number } {
		const sidebarEl = document.querySelector("nav.vertical.nav-droppable.mobile-navigation");
		if (!sidebarEl) return { left: 0, width: 0 };
		const rect = sidebarEl.getBoundingClientRect();
		return { left: rect.left, width: rect.width };
	}
	function getWorkspaceContentAreaHeight(): number {
		// Footer lives under the sidebar only, so the content view just needs a flat bottom margin.
		const boxEl = getWorkspaceContentBox();
		if (!boxEl) return 0;
		const top = boxEl.getBoundingClientRect().top;
		return Math.max(0, window.innerHeight - top - 10);
	}
	$scope.footerBackgroundColor = getWorkspaceFooterBackgroundColor();
	$scope.footerLeft = 0;
	$scope.footerWidth = 0;
	$scope.contentAreaHeight = 0;
	$scope.recomputeWorkspaceFooterOffsets = function (): void {
		const sidebarRect = getWorkspaceSidebarRect();
		$scope.footerLeft = sidebarRect.left;
		$scope.footerWidth = sidebarRect.width;
		$scope.contentAreaHeight = getWorkspaceContentAreaHeight();
		$scope.safeApply();
	};
	window.addEventListener("resize", $scope.recomputeWorkspaceFooterOffsets);
	setTimeout($scope.recomputeWorkspaceFooterOffsets, 0);

	/**
	 * INIT
	 */
	const allowAction = (type: DocumentActionType) => () => {
		const items = $scope.selectedItems();
		if (!workspaceService.isActionAvailable(type, items)) {
			return false;
		}
		return true
	}
	const shouldCache = workspaceService.isLazyMode();
	$scope.documentList = new models.DocumentsListModel($filter).watch($scope,{documents:'openedFolder.documents'});
	$scope.documentListSorted = new models.DocumentsListModel($filter).watch($scope,{documents:'openedFolder.sortedDocuments'});
	$scope.trees = [
    new models.ElementTree(shouldCache, {
      // Renamed for nesting under "Mon espace personnel" (see tree.ts).
      name: lang.translate("workspace.personal.documents"),
      filter: "owner",
      hierarchical: true,
      hidden: false,
      children: [],
      buttons: [
        {
          text: lang.translate("workspace.add.document"),
          action: () => ($scope.display.importFiles = true),
          icon: true,
          workflow: "workspace.create",
          disabled() {
            return false;
          },
        },
      ],
      contextualButtons: [
        {
          text: lang.translate("workspace.move"),
          action: $scope.openMoveView,
          right: "manager",
          allow: allowAction("move"),
        },
        {
          text: lang.translate("workspace.copy"),
          action: $scope.openCopyView,
          right: "read",
          allow: allowAction("copy"),
        },
        {
          text: lang.translate("workspace.move.trash"),
          action: $scope.toTrashConfirm,
          right: "manager",
        },
      ],
    }),
    new models.ElementTree(shouldCache, {
      name: lang.translate("workspace.personal.shared"),
      filter: "shared",
      hierarchical: true,
      hidden: false,
      buttons: [
        {
          text: lang.translate("workspace.add.document"),
          action: () => ($scope.display.importFiles = true),
          icon: true,
          workflow: "workspace.create",
          disabled() {
            if (
              $scope.currentTree.filter == "shared" &&
              $scope.currentTree === $scope.openedFolder.folder
            ) {
              return false;
            }
            let isFolder = $scope.openedFolder.folder instanceof models.Element;
            return isFolder && !$scope.openedFolder.folder.canWriteOnFolder;
          },
        },
      ],
      children: [],
      contextualButtons: [
        {
          text: lang.translate("workspace.move"),
          action: $scope.openMoveView,
          right: "manager",
          allow: allowAction("move"),
        },
        {
          text: lang.translate("workspace.copy"),
          action: $scope.openCopyView,
          right: "read",
          allow: allowAction("copy"),
        },
        {
          text: lang.translate("workspace.move.trash"),
          action: $scope.toTrashConfirm,
          right: "manager",
        },
      ],
    }),
    new models.ElementTree(shouldCache, {
      name: lang.translate("externalDocs"),
      filter: "external",
      get hidden() {
        const tree = $scope.trees.find((e) => e.filter == "external");
        return !tree || tree.children.length == 0;
      },
      buttons: [],
      hierarchical: true,
      children: [],
      contextualButtons: [
        {
          text: lang.translate("workspace.move.trash"),
          action: $scope.toTrashConfirm,
          allow() {
            //trash only files
            return $scope.selectedFolders().length == 0;
          },
        },
      ],
    }),
    new models.ElementTree(shouldCache, {
      name: lang.translate("appDocuments"),
      filter: "protected",
      hidden: false,
      buttons: [
        {
          text: lang.translate("workspace.add.document"),
          action: () => {},
          icon: true,
          workflow: "workspace.create",
          disabled() {
            return true;
          },
        },
      ],
      hierarchical: true,
      children: [],
      contextualButtons: [
        {
          text: lang.translate("workspace.copy"),
          action: $scope.openCopyView,
          right: "read",
          allow: allowAction("copy"),
        },
        {
          text: lang.translate("workspace.move.trash"),
          action: $scope.toTrashConfirm,
          right: "manager",
        },
      ],
    }),
    new models.ElementTree(shouldCache, {
      name: lang.translate("trash"),
      hidden: false,
      buttons: [
        {
          text: lang.translate("workspace.add.document"),
          action: () => {},
          icon: true,
          workflow: "workspace.create",
          disabled() {
            return true;
          },
        },
      ],
      filter: "trash",
      hierarchical: true,
      children: [],
      contextualButtons: [
        {
          text: lang.translate("workspace.trash.restore"),
          action: $scope.restore,
          right: "manager",
        },
        {
          text: lang.translate("workspace.move.trash"),
          action: $scope.deleteConfirm,
          right: "manager",
        },
      ],
    }),
  ];
	$scope.display = {
		nbFiles: 50
	};
	//avoid open lightbox on startup
	setTimeout(() => {
		template.open('lightboxes', 'lightboxes');
		template.open('toaster', 'toaster');
	}, 500)
	//evt emis lors de la maj d un partage
	$rootScope.$on('share-updated', function (_, __) {
		$timeout(() => {
			$scope.reloadFolderContent();
		})
	});
	/**
	 * INIT DELEGATES
	 */
	inits.forEach(cb => cb());

	$scope.safeApply = function (fn) {
		const phase = this.$root.$$phase;
		if (phase == '$apply' || phase == '$digest') {
			if (fn && (typeof (fn) === 'function')) {
				fn();
			}
		} else {
			this.$apply(fn);
		}
	};
	$scope.formatDocumentSize = workspaceService.formatDocumentSize;


	$scope.cancelRequest = function (file) {
		file.request.abort();
	};

	$scope.isUploadedImage = function () {
		return $scope.newFile.chosenFiles.findIndex((file) => {
			const ext = file.extension.toLowerCase();
			return ['png', 'jpg', 'jpeg', 'bmp'].indexOf(ext) > -1
		}) > -1;
	};
	$scope.showCarousel = () => {
		return $scope.currentTree.filter != "external";
	}

	$scope.translate = function (key) {
		return lang.translate(key);
	};

	$scope.longDate = function (dateString) {
		if (!dateString) {
			return moment().format('D MMMM YYYY');
		}

		return moment(dateString.split(' ')[0]).format('D MMMM YYYY');
	}

	$scope.shortDate = function (dateItem) {
		if (!dateItem) {
			return moment().format('L');
		}
		if (typeof dateItem === "number")
			return moment(dateItem).format('L');

		if (typeof dateItem === "string")
			return moment(dateItem.split(' ')[0]).format('L');

		return moment().format('L');
	}
}]);
