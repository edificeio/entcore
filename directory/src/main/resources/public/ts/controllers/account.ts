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

import { ng, idiom as lang, notify, model, Behaviours, http, template, Me, skin, moment, _ } from 'entcore';
import { directory } from '../model';

export const accountController = ng.controller('MyAccount', ['$scope', 'route', ($scope, route) => {
	route({
		editUserInfos: async function(params){
			template.open('account/main', 'account/default-view');
			directory.account = new directory.User({ id: params.id, edit: { infos: true } });
			await init();
			$scope.openView('user-edit', 'user');
		},
		editUser: async function(params){
			template.open('account/main', 'account/default-view');
			directory.account = new directory.User({ id: params.id, edit: { userbook: true, infos: true } });
			await init();
			$scope.openView('user-edit', 'user');
			$scope.openView('userbook-edit', 'userbook');
		},
		themes: async function(){
			directory.account = new directory.User({ id: model.me.userId, edit: { userbook: true, visibility: true } });
			template.open('account/main', 'account/themes');
			await init();
		},
		editMe: async function(params){
			template.open('account/main', 'account/default-view');
			directory.account = new directory.User({ id: model.me.userId, edit: { userbook: true, visibility: true } });
			await init();
			lang.addBundle('/auth/i18n', function () {
				$scope.cguUrl = lang.translate("auth.charter");
			});
			if(model.me.type !== 'ELEVE'){
				directory.account.edit.infos = true;
				$scope.openView('user-edit', 'user');
			}
			else {
				$scope.openView('user-view', 'user');
			}
			$scope.openView('userbook-edit', 'userbook');
		}
	});

	$scope.template = template;

	let conf = { overriding: [] };
	const loadThemeConf = async function(){
		await skin.listSkins();
		conf = skin.themeConf;

		$scope.themes = skin.skins;

		if($scope.themes.length > 1){
			$scope.display.pickTheme = skin.pickSkin;
			http().get('/userbook/preference/theme').done(function(pref){
				if(pref.preference){
					$scope.account.themes[pref.preference] = true;
				}
				else{
					$scope.account.themes[skin.skin] = true;
					http().put('/userbook/preference/theme', skin.skin);
				}
				$scope.$apply();
			})
			if(!$scope.account.themes){
				$scope.account.themes = {};
			}

			$scope.$apply();
		}
	}

	const xhr = new XMLHttpRequest();
	xhr.open('get', '/assets/theme-conf.js');
	xhr.onload = async () => {
		eval(xhr.responseText.split('exports.')[1]);
		const currentTheme = conf.overriding.find(t => t.child === skin.skin);
		if(currentTheme.group){
			$scope.themes = conf.overriding.filter(t => t.group === currentTheme.group);
		}
		else{
			$scope.themes = conf.overriding;
		}

	};
	xhr.send();

	Behaviours.loadBehaviours('directory', function(){
		model.me.workflow.load(['directory'])
	});
    model.me.workflow.load(['zimbra']);
	$scope.hasWorkflowZimbraExpert = () => {
       return model.me.hasWorkflow('fr.openent.zimbra.controllers.ZimbraController|preauth');
    };
	async function init(){
		await directory.account.open();
		await directory.account.loadChildren();
		await directory.account.load();
		$scope.me = model.me;
		directory.account.on('change', function(){
			$scope.$apply();
		});

		$scope.account = directory.account;
		$scope.previousMood = directory.account.mood.id;
		$scope.currentMotto = directory.account.motto;
		$scope.motto = {
			published: true,
			activated: false
		}
		loadThemeConf();
	}

	$scope.display = {};
	$scope.lang = lang;

	$scope.openView = function (view, name) {
		template.open(name, view);
	};
	$scope.containsView = function (name, view) {
		return template.contains(name, view);
	};

	$scope.moods = directory.User.prototype.moods;
		var moods = _.reject($scope.moods, function(mood){
			return mood.id === 'default';
		});
		moods.unshift({ id: "default", icon: "none", text:"userBook.mood.default" });
		$scope.availableMoods = moods;
	$scope.resetPasswordPath = '/auth/reset/password';

	$scope.birthDate = function(birthDate){
		if(birthDate){
			return moment(birthDate).format('D MMMM YYYY');
		}
		return '';
	};

	$scope.setThemePreferences = (themeName: string) => {
		const addedThemes = [];

		for(let name in $scope.account.themes){
			if($scope.account.themes[name]){
				addedThemes.push(name);
			}
		}

		if(addedThemes.length > 1){
			const keptTheme = $scope.themes.find(t => t.parent === 'theme-open-ent').child;
			http().put('/userbook/preference/theme', keptTheme);
		}
		else{
			http().put('/userbook/preference/theme', addedThemes[0]);
		}
	};

	$scope.translate = function(label){
		return lang.translate(label);
	};

	$scope.saveChanges = function(){
		directory.account.saveChanges();
	};

	$scope.openPasswordDialog = function(){
		$scope.display.password = true;
		$scope.account.password = '';
	};

	$scope.openOTPDialog = function() {
		directory.account.generateOTP(function(res) {
			if (res && res.status == 200 && res.data.otp && res.data.otp.length == 8) {
				$scope.account.otp = res.data;
				let content = lang.translate("directory.otp.login").replace("[[login]]", model.me.login).replace("[[otp]]", res.data.otp);
				$scope.account.otp.login = content;
				$scope.display.otp = true;
				$scope.$apply();
			}
		});
	};

	$scope.closePassword = function(){
		$scope.display.password = false;
	};

	$scope.removePicture = function(){
		$scope.account.picture = "";
		$scope.saveChanges();
	}

	$scope.saveLogin = function() {
		directory.account.saveLogin($scope.account.newLoginAlias)
			.done(function(e){
				$scope.account.loginAlias = $scope.account.newLoginAlias;
				$scope.display.login = false;
				$scope.$apply();
			})
			.e400(function(e){
				let errorMsg: string = JSON.parse(e.responseText).error;
				if(errorMsg.includes('already exists') || errorMsg.includes('existe déjà')) {
					notify.error('directory.notify.loginUpdate.error.alreadyExists');
				} else {
					notify.error(errorMsg);
				}
			}.bind(this));
	}

	$scope.saveInfos = function(){
		directory.account.saveInfos();
	};

	$scope.resetPassword = function(url){
		http().post(url, {
				oldPassword: $scope.account.oldPassword,
				password: $scope.account.password,
				confirmPassword: $scope.account.confirmPassword,
				login: $scope.account.login,
				callback: '/userbook/mon-compte'
			})
			.done(function(response){
				if(response.error){
					notify.error('userbook.renewpassword.error');
				}
				else{
					$scope.resetErrors = false;
					$scope.display.password = false;
				}
				$scope.$apply();
			})
	};

	$scope.saveUserbookProperty = function(prop){
		if (prop === 'mood') {
			if (directory.account[prop].id === $scope.previousMood) {
				return;
			}
			$scope.previousMood = directory.account[prop].id;
		}
		directory.account.saveUserbookProperty(prop);
	};

	$scope.publishMotto = function(prop){
		$scope.currentMotto = directory.account.motto;
		$scope.motto.published = true;
		$scope.motto.activated = true;
		$scope.saveUserbookProperty('motto');
	};

	$scope.changeVisibility = function(hobby){
		if(hobby.visibility.toLowerCase() === 'public'){
			hobby.visibility = 'PRIVE';
		}
		else{
			hobby.visibility = 'PUBLIC'
		}

		http().get('api/set-visibility', { value: hobby.visibility, category: hobby.category });
	};

	$scope.changeInfosVisibility = function(info, state){
        if(state.toLowerCase() === 'public'){
            $scope.account.visible[info] = 'prive';
        }
        else{
            $scope.account.visible[info] = 'public';
        }
		http().get('api/edit-user-info-visibility', { info: info, state: $scope.account.visible[info] });
	};

	$scope.resetAvatar = function(){
		directory.account.picture = '';
		directory.account.saveChanges();
	};

	$scope.updateAvatar = function(){
		directory.account.uploadAvatar();
	}

	$scope.longDate = function(dateString){
		return moment(dateString).format('DD MMMM YYYY')
	};

	$scope.passwordComplexity = function(password){
		if(!password)
			return 0

		if(password.length < 6)
			return password.length

		var score = password.length
		if(/[0-9]+/.test(password) && /[a-zA-Z]+/.test(password)){
			score += 5
		}
		if(!/^[a-zA-Z0-9- ]+$/.test(password)) {
			score += 5
		}

		return score
	}

	$scope.translateComplexity = function(password){
		var score = $scope.passwordComplexity(password)
		if(score < 12){
			return lang.translate("weak")
		}
		if(score < 20)
			return lang.translate("moderate")
		return lang.translate("strong")
	}

	$scope.identicalRegex = function(str){
		if(!str)
			return new RegExp("^$")
		return new RegExp("^"+str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")+"$")
	}

	$scope.refreshInput = function(form, inputName){
		form[inputName].$setViewValue(form[inputName].$viewValue)
	}

	$scope.displayPassword = function(account, me) {
		return account.id === me.userId && (!me.federated || (me.federated && account.federatedAddress));
	}

	$scope.displayOTP = function(account, me) {
		return account.id === me.userId && me.federated && me.hasApp;
	}

	$scope.displayChildren = function(currentUser) {
		return currentUser && currentUser.childrenStructure && currentUser.childrenStructure.length && (model.me.type === 'PERSRELELEVE' || model.me.type === 'ENSEIGNANT' || model.me.type === 'PERSEDUCNAT');
	};

	$scope.displayRelatives = function(currentUser) {
		return currentUser && currentUser.relatives.length && (model.me.type === 'ELEVE');
	};

	$scope.generateMergeKey = function() {
		directory.account.generateMergeKey();
	};

	$scope.mergeByKeys = function(account) {
		directory.account.mergeByKeys([account.mergeByKey], function() {
			delete account.mergeByKey;
			$scope.$apply();
		});
	};

	$scope.isMottoChanged = function() {
		return directory.account.motto !== $scope.currentMotto;
	};

	$scope.updateMottoChanged = function() {
		setTimeout(function(){
			if (!$scope.motto.activated) {
				$scope.motto.published = !$scope.isMottoChanged();
			}
			$scope.motto.activated = false;
			$scope.$apply();
		}, 250);
	}

	http().get('/auth/context').done(function(data){
		$scope.passwordRegex = data.passwordRegex;
	})

}]);
