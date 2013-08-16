var timeline = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {

		},
		action : {

		}
	});
	return app;
}();

$(document).ready(function(){
	timeline.init();
});
