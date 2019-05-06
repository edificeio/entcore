import { ng, template, $, model, notify, ui } from 'entcore';
import { UserListDelegate, UserListDelegateScope } from './delegates/userList';
import { MenuDelegate, MenuDelegateScope } from './delegates/menu';
import { EventDelegate } from "./delegates/events";
import { directoryService } from './service';
import { ActionsDelegate, ActionsDelegateScope } from './delegates/actions';
import { UserInfosDelegate, UserInfosDelegateScope } from './delegates/userInfos';
import { UserCreateDelegateScope, UserCreateDelegate } from './delegates/userCreate';
import { ExportDelegateScope, ExportDelegate } from './delegates/userExport';
import { User } from './model';
import { UserFindDelegate, UserFindDelegateScope } from './delegates/userFind';


export interface ClassAdminControllerScope extends UserListDelegateScope, UserInfosDelegateScope, MenuDelegateScope, ActionsDelegateScope, UserCreateDelegateScope, ExportDelegateScope, UserFindDelegateScope {
	safeApply(a?);
	closeLightbox(): void;
	openLightbox(path: string): void;
	lightboxDelegateClose: () => boolean;
	setLightboxDelegateClose(f: () => boolean);
	resetLightboxDelegateClose(): void;
	smoothScrollTo(path: string)
	getProfileColor(user: User): string;
	//Legacy
	import: { csv: File[] }
	display: { importing: boolean }
	resetPasswords(user?: User);
	goToImport();
	importCSV();
}
export const classAdminController = ng.controller('ClassAdminController', ['$scope', ($scope: ClassAdminControllerScope) => {
	// === Init delegates
	EventDelegate($scope);//must be init first
	UserListDelegate($scope);
	MenuDelegate($scope);
	ActionsDelegate($scope);
	UserInfosDelegate($scope);
	UserCreateDelegate($scope);
	ExportDelegate($scope);
	UserFindDelegate($scope);
	// === Init
	const init = async function () {
		const networkPromise = directoryService.getSchoolsForUser(model.me.userId);
		const network = await networkPromise;
		$scope.onSchoolLoaded.next(network);
		console.log("[Directory][Controller] network is ready:", network);
	}
	init();
	setTimeout(() => {
		template.open('lightboxes', 'admin/lightboxes');
	}, 500);
	// === Methods
	$scope.lightboxDelegateClose = () => false;
	$scope.setLightboxDelegateClose = function (f) {
		$scope.lightboxDelegateClose = f;
	}
	$scope.resetLightboxDelegateClose = function () {
		$scope.lightboxDelegateClose = () => false;

	}
	$scope.openLightbox = function (path) {
		template.open('lightbox', path);
	}
	$scope.closeLightbox = function () {
		template.close("lightbox");
	}

	$scope.smoothScrollTo = function (path) {
		const speed = 750; // Durée de l'animation (en ms)
		$('html, body').animate({ scrollTop: $(path).offset().top }, speed);
	}
	//
	let _selection: User[] = [];
	$scope.onSelectionChanged.subscribe(selection => {
		_selection = selection;
	})
	//LEGACY CODE
	$scope.display = {
		importing: false
	}
	$scope.import = { csv: [] };
	$scope.resetPasswords = async function (user) {
		if (!model.me.email) {
			notify.error("classAdmin.reset.error");
			return;
		}
		try {
			if (user) {
				await directoryService.resetPassword([user])
			} else {
				await directoryService.resetPassword(_selection)
			}
			notify.success("directory.admin.reset.code.sent")
		} catch (e) {
			notify.error("directory.admin.reset.code.send.error");
		}
	}
	$scope.goToImport = function () {
		$scope.openLightbox("importCSV")
	}
	$scope.importCSV = async function () {
		if ($scope.display.importing) return;
		try {
			$scope.display.importing = true;
			await directoryService.importFile($scope.import.csv[0], $scope.userList.selectedTab, $scope.selectedClass);
			$scope.queryClassRefresh.next($scope.selectedClass);
			$scope.closeLightbox();
		} finally {
			$scope.display.importing = false;
			$scope.safeApply();
		}
	};
	$scope.getProfileColor = function (user:User) {
		return ui.profileColors.match(user.profile);
	};
}]);