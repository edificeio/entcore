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
				newPassword: $scope.account.password
			})
			.done(function(response){
				$scope.resetPasswordPath = '';
				$scope.resetPasswordContent = response;
				$scope.$apply();
				if(response.indexOf('html') === -1){
					ui.showLightbox();
				}
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

	$scope.updateAvatar = function(){
		var form = new FormData(),
		uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
		form.append("image", $scope.account.photo);
		form.append("name","blablabla");

	if (uuidRegex.test($scope.account.picture)) {
			http.putFile("/workspace/document/" + $scope.account.picture, form, { requestName: 'avatar'})
				.done(function (data) {
					if (data.status == "ok") {
						$scope.account.pictureVersion = $scope.account.pictureVersion + 1;
						$scope.$apply();
						ui.updateAvatar();
					}
				});
		} else {
			http.postFile("/workspace/document?application=userbook&protected=true", form, { requestName: 'avatar'})
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
