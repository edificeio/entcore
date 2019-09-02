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

import { ng, template, idiom as lang, notify, idiom, moment, workspace } from 'entcore';
import { NavigationDelegateScope, NavigationDelegate } from './delegates/navigation';
import { ActionDelegate, ActionDelegateScope } from './delegates/actions';
import { TreeDelegate, TreeDelegateScope } from './delegates/tree';
import { CommentDelegate, CommentDelegateScope } from './delegates/comments';
import { DragDelegate, DragDelegateScope } from './delegates/drag';
import { SearchDelegate, SearchDelegateScope } from './delegates/search';
import { RevisionDelegateScope, RevisionDelegate } from './delegates/revisions';
import { KeyboardDelegate, KeyboardDelegateScope } from './delegates/keyboard';
import { LoolDelegateScope, LoolDelegate } from './delegates/lool';
import { models, workspaceService, DocumentCursor, Document, DocumentCursorParams, CursorUpdate } from "./services";
import { DocumentActionType } from 'entcore/types/src/ts/workspace/services';


declare var ENABLE_LOOL: boolean;
export interface WorkspaceScope extends RevisionDelegateScope, NavigationDelegateScope, TreeDelegateScope, ActionDelegateScope, CommentDelegateScope, DragDelegateScope, SearchDelegateScope, KeyboardDelegateScope, LoolDelegateScope {
	ENABLE_LOOL: boolean;
	//new
	lightboxDelegateClose: () => boolean
	newFile: { chosenFiles: any[] }
	//
	display: { nbFiles: number, importFiles?: boolean, viewFile?: models.Element, share?: boolean }
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
}
export let workspaceController = ng.controller('Workspace', ['$scope', '$rootScope', '$timeout', '$location', '$anchorScroll', 'route', '$route', ($scope: WorkspaceScope, $rootScope, $timeout, $location, $anchorScroll, route, $route) => {
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
	ENABLE_LOOL && LoolDelegate($scope, $route);
	$scope.ENABLE_LOOL = ENABLE_LOOL;
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
	$scope.trees = [{
		name: lang.translate('documents'),
		filter: 'owner',
		hierarchical: true,
		hidden: false,
		children: [],
		buttons: [
			{ text: lang.translate('workspace.add.document'), action: () => $scope.display.importFiles = true, icon: true, workflow: 'workspace.create', disabled() { return false } }
		],
		contextualButtons: [
			{ text: lang.translate('workspace.move'), action: $scope.openMoveView, right: "manager", allow: allowAction("move") },
			{ text: lang.translate('workspace.copy'), action: $scope.openCopyView, right: "read", allow: allowAction("copy") },
			{ text: lang.translate('workspace.move.trash'), action: $scope.toTrashConfirm, right: "manager" }
		]
	}, {
		name: lang.translate('shared_tree'),
		filter: 'shared',
		hierarchical: true,
		hidden: false,
		buttons: [
			{
				text: lang.translate('workspace.add.document'), action: () => $scope.display.importFiles = true, icon: true, workflow: 'workspace.create', disabled() {
					let isFolder = ($scope.openedFolder.folder instanceof models.Element);
					return isFolder && !$scope.openedFolder.folder.canWriteOnFolder
				}
			}
		],
		children: [],
		helpbox: "workspace.help.2",
		contextualButtons: [
			{ text: lang.translate('workspace.move'), action: $scope.openMoveView, right: "manager", allow: allowAction("move") },
			{ text: lang.translate('workspace.copy'), action: $scope.openCopyView, right: "read", allow: allowAction("copy") },
			{ text: lang.translate('workspace.move.trash'), action: $scope.toTrashConfirm, right: "manager" }
		]
	}, {
		name: lang.translate('externalDocs'),
		filter: 'external',
		get hidden() {
			const tree = $scope.trees.find(e => e.filter == "external");
			return !tree || tree.children.length == 0;
		},
		buttons: [],
		hierarchical: true,
		children: [],
		contextualButtons: [
			{
				text: lang.translate('workspace.move.trash'), action: $scope.toTrashConfirm, allow() {
					//trash only files
					return $scope.selectedFolders().length == 0;
				}
			}
		]
	}, {
		name: lang.translate('appDocuments'),
		filter: 'protected',
		hidden: false,
		buttons: [
			{ text: lang.translate('workspace.add.document'), action: () => { }, icon: true, workflow: 'workspace.create', disabled() { return true } }
		],
		hierarchical: true,
		children: [],
		contextualButtons: [
			{ text: lang.translate('workspace.copy'), action: $scope.openCopyView, right: "read", allow: allowAction("copy") },
			{ text: lang.translate('workspace.move.trash'), action: $scope.toTrashConfirm, right: "manager" }
		]
	}, {
		name: lang.translate('trash'),
		hidden: false,
		buttons: [
			{ text: lang.translate('workspace.add.document'), action: () => { }, icon: true, workflow: 'workspace.create', disabled() { return true } }
		],
		filter: 'trash',
		hierarchical: true,
		children: [],
		contextualButtons: [
			{ text: lang.translate('workspace.trash.restore'), action: $scope.restore, right: "manager" },
			{ text: lang.translate('workspace.move.trash'), action: $scope.deleteConfirm, right: "manager" }
		]
	}];
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