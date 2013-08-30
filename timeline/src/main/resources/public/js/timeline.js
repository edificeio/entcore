var timeline = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {

			lastNotifications : '{{#results}}\
									<div class="row line">\
										<div class="cell nine">{{{message}}}</div>\
										<div class="cell three right-magnet"><em>{{#formatDateTime}}{{#date}}{{$date}}{{/date}}{{/formatDateTime}}</em></div>\
										<div class="clear"></div>\
									</div>\
								{{/results}}'

		},
		action : {

			lastNotifications : function (o) {
				$.get(o.url).done(function(response){
					$('#list').html(app.template.render("lastNotifications", response));
				});
			}

		}
	});
	return app;
}();

$(document).ready(function(){
	timeline.init();
	timeline.action.lastNotifications({url : "lastNotifications"});
});
