var appRegistry = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {
			applications : '<div>\
								<a call="allCheckbox" href="checked">{{#i18n}}app.registry.select.all{{/i18n}}</a>\
								<a call="allCheckbox" href="">{{#i18n}}app.registry.unselect.all{{/i18n}}</a>\
								<a call="createRole" href="/role">{{#i18n}}app.registry.createRole{{/i18n}}</a>\
							</div>\
							{{#.}}\
							<div>\
								{{#i18n}}app.registry.application{{/i18n}} : {{name}}<br />\
								{{#i18n}}app.registry.actions{{/i18n}} :\
								<ul>\
								{{#actions}}\
									<li>\
										<input class="select-action" type="checkbox" name="actions[]" value="{{0}}" />\
										{{1}} - {{2}}\
									</li>\
								{{/actions}}\
								</ul>\
							</div>\
							{{/.}}',

			roles : '{{#.}}\
					<div>\
						{{#i18n}}app.registry.role{{/i18n}} : {{name}}<br />\
						{{#i18n}}app.registry.actions{{/i18n}} :\
						<ul>\
						{{#actions}}\
							<li>\
								{{1}} - {{2}}\
							</li>\
						{{/actions}}\
						</ul>\
					</div>\
					{{/.}}',

			createRole : '<form action="{{action}}">\
							<label>{{#i18n}}app.registry.role.name{{/i18n}}</label>\
							<input type="text" name="role" />\
							<input call="addRole" type="button" value="{{#i18n}}app.registry.valid{{/i18n}}" />\
						</form>',

		},
		action : {
			applications : function (o) {
				$.get(o.url)
				.done(function(response){
					var apps = [];
					if (response.status === "ok") {
						for (var key in response.result) {
							var a = response.result[key];
							if (a.actions[0][0] == null) {
								a.actions = [];
							}
							apps.push(a);
						}
						$('#list').html(app.template.render("applications", apps));
					}
				})
				.error(function(data) {app.notify.error(data)});
			},

			allCheckbox : function(o) {
				var selected = o.url;
				$(":checkbox").each(function() {
					this.checked = selected;
				});
			},

			createRole : function(o) {
				$('#form-window').html(app.template.render("createRole", { action : o.url }));
			},

			addRole : function(o) {
				var actions = "",
				form = $(o.target).parents("form");

				$(":checkbox:checked").each(function(i) {
					actions += "," + $(this).val();
				});

				if (actions != "") {
					$.post(form.attr("action"), form.serialize() + "&actions=" + actions.substring(1))
					.done(function(response) {
						$('#form-window').empty();
						appRegistry.action.roles({url : "/roles/actions"});
					})
					.error(function(data) {app.notify.error(data)});
				}
			},

			roles: function(o) {
				$.get(o.url)
				.done(function(response){
					var roles = [];
					if (response.status === "ok") {
						for (var key in response.result) {
							var a = response.result[key];
							if (a.actions[0][0] == null) {
								a.actions = [];
							}
							roles.push(a);
						}
						$('#list').html(app.template.render("roles", roles));
					}
				})
				.error(function(data) {app.notify.error(data)});
			}

		}
	});
	return app;
}();

$(document).ready(function(){
	appRegistry.init();
});
