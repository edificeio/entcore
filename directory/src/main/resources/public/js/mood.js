(function(){
	var Mood = model.widgets.findWidget('mood');

	var availableMoods = ['happy','proud','dreamy','love','tired','angry','worried','sick','joker','sad'];
	Mood.mood = 'default';

	Mood.updateMood = function(){
		http().get('/userbook/api/edit-userbook-info?prop=mood&value=' + Mood.mood.id);
	};

	lang.addBundle('/directory/i18n', function(){
		Mood.title = lang.translate('userBook.mymood');
		Mood.moods = [];
		availableMoods.forEach(function(mood){
			Mood.moods.push({
				icon: mood,
				text: lang.translate('userBook.mood.' + mood),
				id: mood
			})
		});
		http().get('/userbook/api/person').done(function(data){
			var currentMood = data.result[0].mood;
			if(currentMood === 'default'){
				Mood.mood = {
					icon: 'default',
					text: lang.translate('userBook.mood.default'),
					id: 'default'
				};
			}
			else{
				Mood.mood = _(Mood.moods).where({id: currentMood})[0];
			}

			model.widgets.apply();
		});
	});
}());