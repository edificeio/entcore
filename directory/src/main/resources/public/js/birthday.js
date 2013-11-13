(function(){
	var Birthday = LoadedWidgets.findWidget('birthday');

	Birthday.getDay = function(date){
		return moment(date).date();
	}

	Birthday.getMonth = function(date){
		return moment(date).format('MMMM');
	}

	One.get('/userbook/person/birthday').done(function(birthdays){
		Birthday.birthdays = _.filter(birthdays, function(birthday){
			return moment(birthday.birthDate).month() === moment().month();
		});
		LoadedWidgets.apply();
	});


}());