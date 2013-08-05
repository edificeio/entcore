var communication = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {

			confProfils : '{{#groups}}\
								<div>\
									<form action="{{action}}">\
										<div class="left">\
											<label>{{name}}</label>\
											<input type="hidden" name="groupId" value="{{id}}" />\
										</div>\
										<div class="right">\
										{{#profils}}\
											<label>{{.}}</label>\
											<input type="checkbox" name="profil" value="{{.}}" />\
										{{/profils}}\
										</div>\
										<div class="clear">\
											<input type="button" call="confProfilsSubmit" value="{{#i18n}}communication.valid{{/i18n}}" />\
										</div>\
									</form>\
								</div>\
							{{/groups}}',

		},
		action : {

			confProfils: function(o) {
				$.get("/profils")
				.done(function(profils) {
					$.get("/groups/profils")
					.done(function(groups) {
						$('#list').html(app.template.render("confProfils",
								{ action : o.url, profils : profils, groups : groups }));
					})
					.error(function(data) {app.notify.error(data)});
				})
				.error(function(data) {app.notify.error(data)});
			},

			confProfilsSubmit : function(o) {
				var form = $(o.target).parents("form");
				$.post(form.attr("action"), form.serialize())
				.done(function(response) {
					if (response.status === "ok") {
						app.notify.done(app.i18n.bundle["communication.rule.enabled"]);
					} else {
						app.notify.error(response.message);
					}
				})
				.error(function(data) {app.notify.error(data)});
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	communication.init();
});
