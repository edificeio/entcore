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
(function(){
	var Mood = model.widgets.findWidget('mood');

	var availableMoods = ['happy','proud','dreamy','love','tired','angry','worried','sick','joker','sad'];
	Mood.mood = 'default';

	Mood.updateMood = function(){
		http().putJson('/directory/userbook/' + model.me.userId, { mood: Mood.mood.id });
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