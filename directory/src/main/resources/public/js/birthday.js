(function(){
	var Birthday = Model.widgets.findWidget('birthday');
	Birthday.classes = [];
	Birthday.currentClass = '';
	Birthday.emptyList = '';

	Birthday.getDay = function(date){
		return moment(date).date();
	};

	Birthday.getMonth = function(date){
		return moment(date).format('MMMM');
	};

	Birthday.birthdaysInClass = function(){
		return _.filter(Birthday.birthdays, function(birthday){
			return birthday.classes.indexOf(Birthday.currentClass) !== -1;
		});
	};

	Birthday.saveDefaultClass = function(){
		One.get('/userbook/api/edit-userbook-info?prop=userPreferencesBirthdayClass&value=' + Birthday.currentClass);
	};

	One.get('/userbook/user-preferences').done(function(result){
		Birthday.currentClass = result.result[0].userPreferencesBirthdayClass;
		Model.widgets.apply();
	});

	One.get('/userbook/person/birthday').done(function(birthdays){
		lang.addBundle('/directory/i18n', function(){
			Birthday.emptyList = lang.translate('nobirthday');
			Birthday.birthdays = _.filter(birthdays, function(birthday){
				return moment(birthday.birthDate).month() === moment().month();
			});

			Birthday.birthdays = Birthday.birthdays.sort(function(a, b){
				return moment(a.birthDate).date() - moment(b.birthDate).date()
			});

			var classes = [];
			classes = _.pluck(Birthday.birthdays, 'classes');
			classes.forEach(function(classList){
				classList.forEach(function(className){
					if(Birthday.classes.indexOf(className) === -1){
						Birthday.classes.push(className);
					}
				});
			});

			if(!Birthday.currentClass){
				Birthday.currentClass = Birthday.classes[0];
			}

			Model.widgets.apply();
		});
	});
}());