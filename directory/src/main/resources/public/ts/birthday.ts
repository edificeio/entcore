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
import { model, http, idiom as lang } from 'entcore';
import { moment } from 'entcore/libs/moment/moment';
import { _ } from 'entcore/libs/underscore/underscore';

(function(){
	var Birthday = model.widgets.findWidget('birthday');
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
			return _.find(birthday.classes, function(b){ return b[0] === Birthday.currentClass.id }) !== undefined;
		});
	};

	Birthday.saveDefaultClass = function(){
		http().get('/userbook/api/edit-userbook-info?prop=userPreferencesBirthdayClass&value=' + Birthday.currentClass.id);
	};

	http().get('/userbook/user-preferences').done(function(result){
		Birthday.currentClass = { id: result.result[0].userPreferencesBirthdayClass };
		http().get('/userbook/person/birthday').done(function(birthdays){
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
					classList.forEach(function(myClass){
						if(!_.findWhere(Birthday.classes, {id :myClass[0] })){
							Birthday.classes.push({
								name: myClass[1],
								id: myClass[0]
							});
						}
					});
				});

				Birthday.currentClass = _.findWhere(Birthday.classes, { id: Birthday.currentClass.id })
				if(!Birthday.currentClass){
					Birthday.currentClass = Birthday.classes[0];
				}

				model.widgets.apply();
			});
		});
	});


}());