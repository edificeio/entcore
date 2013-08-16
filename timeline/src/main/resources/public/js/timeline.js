var timeline = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {

			lastNotifications : '<div>\
								{{#results}}\
									<span>{{#date}}{{$date}}{{/date}} - {{{message}}}</span><br />\
								{{/results}}\
								</div>'

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
	timeline.action.lastNotifications({url : "/lastNotifications"});
});
