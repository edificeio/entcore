(function(){
	var Birthday = LoadedWidgets.findWidget('birthday');

	One.get('/userbook/person/birthday').done(function(birthdays){
		Birthday.birthdays = birthdays;
	});

	LoadedWidgets.apply();
}());