import { model } from './entcore';

var moment = require('moment');
var _ = require('underscore');

export var calendar = {
    setCalendar: function(cal){
        model.calendar = cal;
    },
	getHours: function(scheduleItem, day){
		var startTime = 7;
		var endTime = 20;

		if(scheduleItem.beginning.dayOfYear() === day.index){
			startTime = scheduleItem.beginning.hours();
		}

		if(scheduleItem.end.dayOfYear() === day.index){
			endTime = scheduleItem.end.hours();
		}

		return {
			startTime: startTime,
			endTime: endTime
		}
	},
	TimeSlot: function(data){
	},
	ScheduleItem: function(){

	},
	Day: function(data){
		var day = this;
		this.collection(calendar.ScheduleItem, {
			beforeCalendar: function(){
				return this.filter(function(scheduleItem){
					return scheduleItem.beginning.hour() < calendar.startOfDay;
				}).length;
			},
			afterCalendar: function(){
				return this.filter(function(scheduleItem){
					return scheduleItem.end.hour() > calendar.endOfDay;
				}).length;
			},
			scheduleItemWidth: function(scheduleItem){
				var concurrentItems = this.filter(function(item){
					return item.beginning.unix() < scheduleItem.end.unix() && item.end.unix() > scheduleItem.beginning.unix()
				});
				var maxGutter = 0
				_.forEach(concurrentItems, function(item){
					if(item.calendarGutter && item.calendarGutter > maxGutter){
						maxGutter = item.calendarGutter
					}
				});
				maxGutter++;
				return Math.floor(99 / maxGutter);
			}
		});
		this.collection(calendar.TimeSlot);
		for(var i = calendar.startOfDay; i < calendar.endOfDay; i++){
			this.timeSlots.push(new calendar.TimeSlot({ start: i, end: i+1 }))
		}
	},
	Calendar: function(data){
		if(!data.year){
			data.year = moment().year();
		}
		this.week = data.week;
		this.year = data.year;

	    // change of year in moment is buggy (last/first week is on the wrong year)
        // weird syntax is a workaround
        this.dayForWeek = moment(
            this.year +
            "-W" + (this.week < 10 ? "0" + this.week : this.week) +
            "-1")

		var that = this;

		this.collection(calendar.Day, {
			sync: function(){
				function dayOfYear(dayOfWeek){
					var week = that.dayForWeek.week();
					var year = that.dayForWeek.year();
					if(dayOfWeek === 0){
                        return moment(that.dayForWeek).day(dayOfWeek).add(1, 'w').dayOfYear()
					}
					return moment(that.dayForWeek).day(dayOfWeek).dayOfYear()
				}

				that.days.load([{ name: 'monday', index:  dayOfYear(1) },
					{ name: 'tuesday', index: dayOfYear(2) },
					{ name: 'wednesday', index: dayOfYear(3) },
					{ name: 'thursday', index: dayOfYear(4) },
					{ name: 'friday', index: dayOfYear(5) },
					{ name: 'saturday', index: dayOfYear(6) },
					{ name: 'sunday', index: dayOfYear(0) }]);

				that.firstDay = moment(that.dayForWeek).day(1)
			}
		});

		this.days.sync();

		this.collection(calendar.TimeSlot);
		for(var i = calendar.startOfDay; i < calendar.endOfDay; i++){
			this.timeSlots.push(new calendar.TimeSlot({ beginning: i, end: i+1 }))
		}
	},
	startOfDay: 7,
	endOfDay: 20,
	dayHeight: 40,
	init: function(){
		model.makeModels(calendar);
		model.calendar = new calendar.Calendar({ week: moment().week() });
	}
};

calendar.Calendar.prototype.addScheduleItems = function(items){
	var schedule = this;
	items = _.filter(items, function (item) {
	    return moment(item.end).year() >= schedule.firstDay.year()
	});
	_.filter(items, function (item) {
	    return moment(item.end).month() >= schedule.firstDay.month() || moment(item.end).year() > schedule.firstDay.year()
	});
	items.forEach(function(item){
		var startDay = moment(item.beginning);
		var endDay = moment(item.end);

		var refDay = moment(schedule.firstDay)
		schedule.days.forEach(function(day){
			if(startDay.isBefore(moment(refDay).endOf('day')) && endDay.isAfter(moment(refDay).startOf('day')))
				day.scheduleItems.push(item);
			refDay.add('day', 1);
		});
	});
};

calendar.Calendar.prototype.setDate = function(momentDate){
	this.dayForWeek = momentDate;
	this.days.sync();
	this.trigger('date-change');
};

calendar.Calendar.prototype.clearScheduleItems = function(){
	this.days.forEach(function(day){
		day.scheduleItems.removeAll();
	});
};
if (!window.entcore) {
    window.entcore = {};
}
window.entcore.calendar = calendar;