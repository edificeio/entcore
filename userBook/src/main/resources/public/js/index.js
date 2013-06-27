var index = function(){
	var app = Object.create(oneApp);
	app.scope = "#userBook";
	app.define ({
		template : {
			picture : '<img src="{{picture}}">',
			userData : "<p><span>{{firstName}} {{lastName}}</span></p>\
						{{#attributes}}\
							<p><span>{{label}} : </span><span>{{value}}</span></p>\
						{{/attributes}}",
			motto : "<p><span>{{label}} : </span><span>{{value}}</span></p>",
			interests : "{{#interests}}\
							<p><span>{{label}} : </span>\
							<span>{{#values}}{{value}}, {{/values}}</span>\
							</p>\
						{{/interests}}",
			health : "<p><span>{{health}}</span></p>"
		},
		action : {
			load : function(o) {
				$.get(o.url).done(function(response){
					$('#picture').html(app.template.render("picture",response));
					$('#data').html(app.template.render("userData",response.userData));
					$('#motto').html(app.template.render("motto",response.motto));
					var regExp = new RegExp("(, <)","g");
					$('#interests').html(app.template.render("interests",response).replace(regExp,"<"));
					$('#health').html(app.template.render("health",response));
				})
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	index.init();
});