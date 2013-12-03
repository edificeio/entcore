(function(){
	var Calendar = LoadedWidgets.findWidget('calendar');

	Calendar.today = function(format){
		return moment().format(format);
	};

	Calendar.month = [];
	for(var i = 0; i < moment().daysInMonth(); i++){
		Calendar.month[i] = moment().date(i + 1);
	}

	Calendar.previousMonth = [];
	var currentDay = moment().date(0);
	while(currentDay.week() === moment().date(1).week()){
		Calendar.previousMonth.push(currentDay);
		currentDay = moment(currentDay).date(currentDay.date() - 1);
	}
	Calendar.previousMonth.reverse();

	Calendar.nextMonth = [];
	currentDay = moment().date(moment().daysInMonth() + 1)
	while(currentDay.week() ===  moment().date(moment().daysInMonth()).week()){
		Calendar.nextMonth.push(currentDay);
		currentDay = moment(currentDay).date(currentDay.date() + 1);
	}

	Calendar.getDay = function(date){
		return date.day();
	}

	Calendar.getDayInMonth = function(date){
		return date.date();
	}

	Calendar.todayDate = function(){
		return moment().date();
	}

	Calendar.getSeason = function(){
		var currentYear = moment().year() + '-';
		var seasons = [ {
			start: moment(currentYear + '03-21'), end: moment(currentYear + '06-20'), name: 'calendar.spring'
		}, {
			name: 'calendar.summer', start: moment(currentYear + '06-21'), end: moment(currentYear + '09-22')
		}, {
			name: 'calendar.fall', start: moment(currentYear + '09-23'), end: moment(currentYear + '12-20')
		}, {
			name: 'calendar.winter', start: moment(currentYear + '12-21'), end: moment(currentYear + '03-20')
		} ]

		return lang.translate(_.find(seasons, function(season){
			return season.start.dayOfYear() <= moment().dayOfYear() && moment().dayOfYear() <= season.end.dayOfYear();
		}).name);
	}

	LoadedWidgets.apply();
}());