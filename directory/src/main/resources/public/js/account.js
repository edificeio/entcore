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

	// TODO : extract in conf system
	return person;
};

function Account($scope, http, lang, date, notify){
	$scope.account = {};
	$scope.moods = ['default','happy','proud','dreamy','love','tired','angry','worried','sick','joker','sad'];

	http.get('api/person')
		.done(function(data){
			$scope.account = personDataExtractor(data);
			$scope.$apply();
			messenger.requireResize();
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

	$scope.pandaStatus = function(mood){
		return lang.translate('userBook.mood.' + mood);
	};

	$scope.saveHobby = function(hobby){
		http.get('api/edit-userbook-info?category=' + hobby.category + '&value=' + hobby.values);
	};

	$scope.saveProperty = function(property, value){
		http.get('api/edit-userbook-info?prop=' + property + '&value=' + value);
	};

	$scope.saveMail = function(value){
		http.get('api/edit-user-info?prop=email&value=' + value)
			.e400(function(){
				notify.error('mail.invalid')
			});
	};

	$scope.password = function(){
		One.get("/auth/reset/password")
			.done(function(response) {
				$('#change-password').html(response);
				ui.showLightbox();
			});
	};

	$scope.closePassword = function(){
		ui.hideLightbox();
	};

	$scope.submitPassword = function(){
		var form = $("#changePassword");
		One.post(form.attr('action'), form.serialize())
			.done(function(response) {
				$('#change-password').html(response);
				if(response.indexOf('html') === -1){
					ui.showLightbox();
				}
			});
	};

	$scope.changeVisibility = function(hobby){
		if(hobby.visibility === 'public'){
			hobby.visibility = 'PRIVE';
		}
		else{
			hobby.visibility = 'PUBLIC'
		}

		One.get('api/set-visibility', { value: hobby.visibility, category: hobby.category });
	}
}

function manageEditable(){
	var sendPhoto =  function(elem, files) {
		var form = new FormData();
		form.append("image", $('#upload-form').find('input[type="file"]')[0].files[0]);
		form.append("name","blablabla");


		One.postFile("document?application=userbook&protected=true", form, {})
			.done(function (data) {
				if (data.status == "ok") {
					account.action.editUserBookInfo("api/edit-userbook-info?prop=picture&value=" + data._id);
					$('img[class="avatar"]')[0].setAttribute("src", "document/" + data._id);
					messenger.updateAvatar();
				}
			});
	}
	$('#avatar').on('change', function(){
		sendPhoto(this);
	})
}

$(document).ready(function(){

	$('body').on('click','input.cancel', function(){ui.hideLightbox();});
	$('body').on('click','input.submit', function(){ui.hideLightbox();});
	$('body').on('submit', '#changePassword',function(event){
		event.preventDefault();
		account.action.passwordSubmit(event);
		return false;
	});

	ui.hideLightbox();
});
