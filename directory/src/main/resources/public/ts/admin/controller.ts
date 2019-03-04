import { ng, template, $, model, notify } from 'entcore';
import { UserListDelegate, UserListDelegateScope } from './delegates/userList';
import { MenuDelegate, MenuDelegateScope } from './delegates/menu';
import { EventDelegate } from "./delegates/events";
import { directoryService } from './service';
import { ActionsDelegate, ActionsDelegateScope } from './delegates/actions';
import { UserInfosDelegate, UserInfosDelegateScope } from './delegates/userInfos';
import { UserCreateDelegateScope, UserCreateDelegate } from './delegates/userCreate';
import { User } from './model';


export interface ClassAdminControllerScope extends UserListDelegateScope, UserInfosDelegateScope, MenuDelegateScope, ActionsDelegateScope, UserCreateDelegateScope {
	safeApply(a?);
	closeLightbox(): void;
	openLightbox(path: string): void;
	lightboxDelegateClose: () => boolean;
	setLightboxDelegateClose(f: () => boolean);
	resetLightboxDelegateClose(): void;
	smoothScrollTo(path: string)
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
	//TODO
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
		try {
			if (user) {
				await directoryService.resetPassword([user])
			} else {
				await directoryService.resetPassword(_selection)
			}
			notify.info("directory.admin.reset.code.sent")
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
			await directoryService.importFile($scope.import.csv[0], $scope.userList.selectedTab, $scope.selectedClass.id);
			$scope.onClassRefreshed.next($scope.selectedClass);
			$scope.closeLightbox();
		} finally {
			$scope.display.importing = false;
			$scope.safeApply();
		}
	};
	/*
		directory.network.sync();
		directory.network.one('schools.sync', function () {
			directory.network.schools.forEach(function (school) {
				school.sync();
			});
		}
		else {
			directory.classAdmin.users.forEach(function (user) {
				if (user.type === $scope.display.show) {
					user.selected = false;
				}
			});
		}
	};

	

	$scope.grabUser = function (user) {
		directory.classAdmin.grabUser(user);
		notify.info('user.added');
		ui.hideLightbox();
		$scope.newUser = new directory.User();
	};

	$scope.createUser = function () {
		ui.hideLightbox();
		var user = $scope.newUser;
		user.type = $scope.display.show;
		directory.classAdmin.addUser(user);
		});
	
		$scope.classAdmin = directory.classAdmin;
		$scope.users = directory.classAdmin.users;
		$scope.newUser = new directory.User();
		$scope.import = {};
		$scope.me = model.me;
		$scope.display = {};
	
		directory.network.on('classrooms-sync', function () {
			$scope.classrooms = _.filter(directory.network.schools.allClassrooms(), function (classroom) {
				return model.me.classes.indexOf(classroom.id) !== -1;
			});
			directory.classAdmin.sync();
			$scope.$apply();
		});
	
		$scope.viewsContainers = {};
		$scope.openView = function (view, name) {
			if (name === 'lightbox') {
				ui.showLightbox();
			}
			var viewsPath = '/directory/public/template/';
			$scope.viewsContainers[name] = viewsPath + view + '.html';
		};
	
		$scope.containsView = function (name, view) {
			var viewsPath = '/directory/public/template/';
			return $scope.viewsContainers[name] === viewsPath + view + '.html';
		};
	
		directory.classAdmin.on('change, users.change', function () {
			$scope.display.importing = false;
			if (!$scope.$$phase) {
				$scope.$apply('users');
				$scope.$apply('display');
				$scope.$apply('classAdmin');
			}
		});
	
		$scope.shortDate = function (dateString) {
			return moment(dateString).format('D/MM/YYYY');
		};
	
		$scope.display = {
			show: 'Student',
			relative: 'Relative',
			selectAll: false,
			limit: 20,
			relativeSearch: ''
		};
	
		$scope.show = function (tab) {
			directory.classAdmin.users.deselectAll();
			$scope.display.show = tab;
			$scope.display.limit = 20;
			if (tab === 'Relative') {
				$scope.display.relative = 'Student';
			}
			else {
				$scope.display.relative = 'Relative';
			}
		};
	
		$scope.showMore = function () {
			$scope.display.limit += 20;
		};
	
		$scope.clearSearch = function () {
			$scope.display.relativeSearch = '';
			$scope.updateFoundRelatives();
		};
	
		$scope.updateFoundRelatives = function () {
			if (!$scope.display.relativeSearch) {
				$scope.foundRelatives = '';
				return;
			}
			$scope.foundRelatives = _.filter(
				directory.classAdmin.users.match($scope.display.relativeSearch), function (user) {
					return user.type === $scope.display.relative && $scope.newUser.relatives.indexOf(user) === -1;
				}
			);
		};
	
		$scope.addRelative = function () {
			$scope.newUser.relatives.push($scope.newUser.newRelative);
			$scope.clearSearch();
		};
	
		$scope.removeRelative = function (relative) {
			$scope.newUser.removeRelative(relative);
		};
	
		
	
		$scope.saveClassInfos = function () {
			directory.classAdmin.saveClassInfos();
		};
	
		$scope.importCSV = function () {
			$scope.display.importing = true;
			directory.classAdmin.importFile($scope.import.csv[0], $scope.display.show.toLowerCase());
			ui.hideLightbox();
		};
	
		$scope.grabUser = function (user) {
			directory.classAdmin.grabUser(user);
			notify.info('user.added');
			ui.hideLightbox();
			$scope.newUser = new directory.User();
		};
	
		$scope.createUser = function () {
			ui.hideLightbox();
			var user = $scope.newUser;
			user.type = $scope.display.show;
			directory.classAdmin.addUser(user);
			$scope.newUser = new directory.User();
			$('#lastName').focus();
			notify.info('user.added');
		};
	
		$scope.addUser = function () {
			$scope.clearSearch();
			directory.directory.users.searchDirectory($scope.newUser.lastName, '', function () {
				$scope.existingMatchs = directory.usersMatch.call(directory.directory.users, $scope.newUser.firstName + ' ' + $scope.newUser.lastName);
				if ($scope.existingMatchs.length > 0 && $scope.display.show === 'Student') {
					$scope.openView('link-user', 'lightbox');
					$scope.$apply();
					return;
				}
				$scope.$apply(function () {
					$scope.createUser();
				});
			});
		};
	
		$scope.blockUsers = function () {
			directory.classAdmin.blockUsers(true);
		};
	
		$scope.unblockUsers = function () {
			directory.classAdmin.blockUsers(false);
		};
	
		$scope.resetPasswords = function () {
			if (!model.me.email) {
				notify.error('error.missing.mail');
			}
			else {
				notify.info('info.passwords.sent');
				directory.classAdmin.resetPasswords();
			}
		};
	
		$scope.removeUsers = function () {
			directory.classAdmin.users.removeSelection();
			$scope.display.confirmRemove = false;
		};
	
		$scope.uploadPhoto = function () {
			$scope.newUser.uploadAvatar()
		};
	
	
	};
	*/

}]);