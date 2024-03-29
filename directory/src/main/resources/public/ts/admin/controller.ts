import { ng, template, $, model, notify, ui } from 'entcore';
import { UserListDelegate, UserListDelegateScope } from './delegates/userList';
import { MenuDelegate, MenuDelegateScope } from './delegates/menu';
import { EventDelegate, ITracker, TRACK } from "./delegates/events";
import { directoryService } from './service';
import { ActionsDelegate, ActionsDelegateScope } from './delegates/actions';
import { UserInfosDelegate, UserInfosDelegateScope } from './delegates/userInfos';
import { UserCreateDelegateScope, UserCreateDelegate } from './delegates/userCreate';
import { ExportDelegateScope, ExportDelegate } from './delegates/userExport';
import { ClassRoom, User } from './model';
import { UserFindDelegate, UserFindDelegateScope } from './delegates/userFind';
import { ChooseClassDelegate, ChooseClassDelegateScope } from './delegates/choose-class';


export interface ClassAdminControllerScope extends UserListDelegateScope, UserInfosDelegateScope, MenuDelegateScope, ActionsDelegateScope, UserCreateDelegateScope, ExportDelegateScope, UserFindDelegateScope, ChooseClassDelegateScope {
	safeApply(a?);
	onToasterUnlinkUsers();
	closeLightbox(): void;
	openLightbox(path: string): void;
	lightboxDelegateClose: () => boolean;
	setLightboxDelegateClose(f: () => boolean);
	resetLightboxDelegateClose(): void;
	smoothScrollTo(path: string)
	getProfileColor(user: User): string;
	optionLabelFor(classroom:ClassRoom):string;
	showEditClassName():void;
	hideEditClassName(cancel:boolean):void;
	//Legacy
	import: { csv: File[] };
	display: { importing: boolean; editClassName: boolean };
	save: { editClassName?: string };
	resetPasswords(user?: User);
	goToImport();
	importCSV();
	hasONDE():boolean
}
export const classAdminController = ng.controller('ClassAdminController', ['$scope', 'tracker', ($scope: ClassAdminControllerScope, tracker:ITracker) => {
	// === Init delegates
	EventDelegate($scope, tracker);//must be init first
	UserListDelegate($scope);
	MenuDelegate($scope);
	ActionsDelegate($scope);
	UserInfosDelegate($scope);
	UserCreateDelegate($scope);
	ExportDelegate($scope);
	UserFindDelegate($scope);
	ChooseClassDelegate($scope);
	// === Init
	const init = async function () {
		const networkPromise = directoryService.getSchoolsForUser(model.me.userId);
		const network = await networkPromise;
		$scope.onSchoolLoaded.next(network);
		if( (!network || !network.length
			  		  || (network.length==1 && (!network[0].classrooms || !network[0].classrooms.length)))
			  && model.me.structures && model.me.structures.length>0 ) {
			template.open('lightbox', 'admin/class/choose-class');
		}
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

	$scope.onToasterUnlinkUsers = function () {
		// #47174, Track this event
		$scope.tracker.trackEvent(TRACK.event, TRACK.USER_BLOCK.action, TRACK.name(TRACK.USER_BLOCK.REMOVE_CLASS));
		$scope.openLightbox('admin/actions/unlink');
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
		importing: false,
		editClassName: false
	}
	$scope.save = {};
	$scope.import = { csv: [] };
	$scope.resetPasswords = async function (user) {
		if (!model.me.email) {
			notify.error("classAdmin.reset.error");
			return;
		}
		try {
			if (user) {
                // #47174, Track this event
                $scope.tracker.trackEvent( TRACK.event, TRACK.AUTH_MODIFICATION.action, TRACK.name(TRACK.AUTH_MODIFICATION.PWD_USER, user.type), 1 );
				await directoryService.resetPassword([user])
			} else {
                // #47174, Track this event //TODO user.type ??
				$scope.tracker.trackEvent( TRACK.event, TRACK.AUTH_MODIFICATION.action, TRACK.name(TRACK.AUTH_MODIFICATION.PWD, $scope.userList.selectedTab), _selection.length );
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
			// #47174, Track this event
			$scope.tracker.trackEvent( TRACK.event, TRACK.USERS_IMPORT.action, TRACK.USERS_IMPORT.IMPORT );
		} catch(e) {
			// #47174, Track this event
			$scope.tracker.trackEvent( TRACK.event, TRACK.USERS_IMPORT.action, TRACK.name(TRACK.USERS_IMPORT.ERROR) );
		} finally {
			$scope.display.importing = false;
			$scope.safeApply();
		}
	};
	$scope.getProfileColor = function (user:User) {
		return ui.profileColors.match(user.profile);
	};
	$scope.hasONDE = function(){
		const currentLanguage = (window as any).currentLanguage || window.navigator.language;
		return currentLanguage=="fr";
	}
	$scope.optionLabelFor = function(classroom:ClassRoom) {
		if( $scope.belongsToMultipleSchools() ) {
			return `${classroom.name} - ${$scope.selectedSchoolName(classroom)}`;
		}
		return classroom.name;
	}
	$scope.showEditClassName = function() {
		$scope.display.editClassName = true;
		$scope.save.editClassName = $scope.selectedClass.name;
	}
	$scope.hideEditClassName = function(save:boolean) {
		if( save ) {
			const backup = $scope.selectedClass;
			backup.name = $scope.save.editClassName;
			// Save and update collection
			directoryService.saveClassInfos(backup)
			.then( c => {
				// Here, $scope may have changed... $scope.selectedClass may have changed too... Big mess.
				const idx = $scope.classrooms.findIndex( e => e.id===$scope.selectedClass.id );
				if( 0<=idx && idx<$scope.classrooms.length ) {
					$scope.classrooms[idx].name = backup.name;
				}
				$scope.safeApply('classrooms');
			});
		}
		$scope.save.editClassName = undefined;
		$scope.display.editClassName = false;
	}
}]);