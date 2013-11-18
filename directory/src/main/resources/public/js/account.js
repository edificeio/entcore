var personDataExtractor = function(d) {
	var person = d.result[0];

	person['hobbies'] = [];
	d.result[0].category.forEach(function(c,index){
		person['hobbies'].push({
			"category" : c,
			"values" : d.result[0].values[index],
			"visibility" : d.result[0].visibility[index].toLowerCase()
		});
	});

	person['relations'] = [];
	_.values(d.result).forEach(function(o){
		person['relations'].push(_.pick(o, 'relatedId', 'relatedName','relatedType'));
	});

	person.picture = person.photo;
	person.visible = {};
	person.visible.email = ($.inArray("SHOW_EMAIL", person.visibleInfos) >= 0) ? "public" : "prive";
	person.visible.mail = ($.inArray("SHOW_MAIL", person.visibleInfos) >= 0) ? "public" : "prive";
	person.visible.phone = ($.inArray("SHOW_PHONE", person.visibleInfos) >= 0) ? "public" : "prive";
	person.visible.birthdate = ($.inArray("SHOW_BIRTHDATE", person.visibleInfos) >= 0) ? "public" : "prive";
	person.visible.health = ($.inArray("SHOW_HEALTH", person.visibleInfos) >= 0) ? "public" : "prive";
	// TODO : extract in conf system
	return person;
};

function MyAccount($scope, http, lang, date, notify, _){
	$scope.account = {};
	var moods = ['default','happy','proud','dreamy','love','tired','angry','worried','sick','joker','sad'];
	$scope.moods = [];

	moods.forEach(function(mood){
		$scope.moods.push({
			icon: mood + '-panda',
			text: lang.translate('userBook.mood.' + mood),
			id: mood
		})
	});

	$scope.resetPasswordPath = '/auth/reset/password';

	http.get('api/person')
		.done(function(data){
			$scope.account = personDataExtractor(data);
			$scope.account.pictureVersion = 0;
			$scope.account.mood = _($scope.moods).where({id: $scope.account.mood})[0];
			$scope.account.photo = {};
			$scope.$apply();
		});

	$scope.birthDate = function(birthDate){
		if(birthDate){
			return date.format(birthDate, 'D MMMM YYYY');
		}
		return '';
	};

	$scope.translate = function(label){
		return lang.translate(label);
	};

	$scope.updateMood = function(){
		http.get('api/edit-userbook-info?prop=mood&value=' + $scope.account.mood.id);
	}

	$scope.saveHobby = function(hobby){
		http.get('api/edit-userbook-info', hobby);
	};

	$scope.saveProperty = function(property){
		//new lines formatting
		var savedValue = $scope.account[property].replace(/[\n]/g, '%0a');
		http.get('api/edit-userbook-info?prop=' + property + '&value=' + savedValue);
	};

	$scope.saveMail = function(value){
		http.get('api/edit-user-info?prop=email&value=' + value)
			.e400(function(){
				notify.error('mail.invalid')
			});
	};

	$scope.openPasswordDialog = function(){
		ui.showLightbox();
	};

	$scope.closePassword = function(){
		ui.hideLightbox();
	};

	$scope.resetPassword = function(url){
		http.post(url, {
				oldPassword: $scope.account.oldPassword,
				password: $scope.account.password,
				confirmPassword: $scope.account.password,
				login: $scope.account.login,
				callback: '/userbook/mon-compte'
			})
			.done(function(response){
				if(response.indexOf('html') === -1){
					notify.error('Le formulaire contient des erreurs');
				}
				else{
					$scope.resetErrors = false;
					ui.hideLightbox();
				}
				$scope.$apply();
			})
	};

	$scope.changeVisibility = function(hobby){
		if(hobby.visibility.toLowerCase() === 'public'){
			hobby.visibility = 'PRIVE';
		}
		else{
			hobby.visibility = 'PUBLIC'
		}

		One.get('api/set-visibility', { value: hobby.visibility, category: hobby.category });
	};

	$scope.changeInfosVisibility = function(info, state){
        if(state.toLowerCase() === 'public'){
            $scope.account.visible[info] = 'prive';
        }
        else{
            $scope.account.visible[info] = 'public';
        }
		One.get('api/edit-user-info-visibility', { info: info, state: $scope.account.visible[info] });
	};

	$scope.updateAvatar = function(){
		var form = new FormData(),
		uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
		thumbs = "thumbnail=290x290&thumbnail=82x82&thumbnail=48x48";
		form.append("image", $scope.account.photo);
		form.append("name","blablabla");

	if (uuidRegex.test($scope.account.picture)) {
    http.putFile("/workspace/document/" + $scope.account.picture + "?" + thumbs,
        form, { requestName: 'avatar'})
				.done(function (data) {
					if (data.status == "ok") {
						$scope.account.pictureVersion = $scope.account.pictureVersion + 1;
						$scope.$apply();
						ui.updateAvatar();
					}
				});
		} else {
      http.postFile("/workspace/document?application=userbook&protected=true&" + thumbs,
          form, { requestName: 'avatar'})
        .done(function (data) {
					if (data.status == "ok") {
						$scope.account.picture = data._id;
						$scope.saveProperty('picture');
						$scope.$apply();
						ui.updateAvatar();
					}
				});
		}
	}
}
