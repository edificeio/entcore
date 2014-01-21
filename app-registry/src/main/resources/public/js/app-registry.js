var appRegistry = function(){
	var app = Object.create(protoApp);
	app.scope = "#main";
	app.define ({
		template : {
			applications : '<div>\
								<a call="allCheckbox" href="checked">{{#i18n}}app.registry.select.all{{/i18n}}</a>\
								<a call="allCheckbox" href="">{{#i18n}}app.registry.unselect.all{{/i18n}}</a>\
								<a call="createRole" href="role">{{#i18n}}app.registry.createRole{{/i18n}}</a>\
								<a call="createExternalApp" href="application/external">{{#i18n}}app.registry.createExternalApp{{/i18n}}</a>\
							</div>\
							{{#.}}\
							<div>\
								{{#i18n}}app.registry.application{{/i18n}} : <a call="application" href=\"application/conf/{{id}}\">{{name}}</a><br />\
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

			authorizeGroups : '{{#groups}}\
								<div>\
									<form action="{{action}}">\
										<div class="left">\
											<label>{{name}}</label>\
											<input type="hidden" name="groupId" value="{{id}}" />\
										</div>\
										<div class="right">\
										{{#roles}}\
											<label>{{rolename}}</label>\
											<input type="checkbox" name="roleIds" value="{{roleid}}" {{checked}}/>\
											<br />\
										{{/roles}}\
										</div>\
										<div class="clear">\
											<input type="button" call="authorizeGroupsSubmit" value="{{#i18n}}app.registry.valid{{/i18n}}" />\
										</div>\
									</form>\
								</div>\
							{{/groups}}',

			createRole : '<form action="{{action}}">\
							<label>{{#i18n}}app.registry.role.name{{/i18n}}</label>\
							<input type="text" name="role" />\
							<input call="addRole" type="button" value="{{#i18n}}app.registry.valid{{/i18n}}" />\
						</form>',

			application : '<span>{{name}}</span>\
							<form action="application/conf">\
								<input type="hidden" name="applicationId" value="{{id}}" />\
								<input type="hidden" name="grantType" value="authorization_code" />\
								<label>{{#i18n}}app.registry.application.display.name{{/i18n}}</label>\
                                <input type="text" name="displayName" value="{{displayName}}" /><br />\
								<label>{{#i18n}}app.registry.application.secret{{/i18n}}</label>\
								<input type="text" name="secret" value="{{secret}}" /><br />\
								<label>{{#i18n}}app.registry.application.address{{/i18n}}</label>\
								<input type="text" name="address" value="{{address}}" /><br />\
								<label>{{#i18n}}app.registry.application.icon{{/i18n}}</label>\
								<input type="text" name="icon" value="{{icon}}" /><br />\
								<label>{{#i18n}}app.registry.application.target{{/i18n}}</label>\
								<input type="text" name="target" value="{{target}}" /><br />\
								<label>{{#i18n}}app.registry.application.scope{{/i18n}}</label>\
                                <input type="text" name="scope" value="{{scope}}" /><br />\
								<input call="applicationConf" type="button" value="{{#i18n}}app.registry.valid{{/i18n}}" />\
							</form>',

			createExternalApp : '<h3>{{#i18n}}app.registry.createExternalApp{{/i18n}}</h3>\
							<form action="{{action}}">\
								<input type="hidden" name="grantType" value="authorization_code" />\
								<label>{{#i18n}}app.registry.application{{/i18n}}</label>\
								<input type="text" name="name" /><br />\
								<label>{{#i18n}}app.registry.application.display.name{{/i18n}}</label>\
                                <input type="text" name="displayName" value="{{displayName}}" /><br />\
								<label>{{#i18n}}app.registry.application.secret{{/i18n}}</label>\
								<input type="text" name="secret" /><br />\
								<label>{{#i18n}}app.registry.application.address{{/i18n}}</label>\
								<input type="text" name="address" /><br />\
								<label>{{#i18n}}app.registry.application.icon{{/i18n}}</label>\
								<input type="text" name="icon" /><br />\
								<label>{{#i18n}}app.registry.application.target{{/i18n}}</label>\
								<input type="text" name="target" /><br />\
								<label>{{#i18n}}app.registry.application.scope{{/i18n}}</label>\
                                <input type="text" name="scope" value="{{scope}}" /><br />\
								<input call="createExternalAppSubmit" type="button" value="{{#i18n}}app.registry.valid{{/i18n}}" />\
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
						appRegistry.action.roles({url : "roles/actions"});
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
			},

			authorizeGroups: function(o) {
				$.get("roles")
				.done(function(response) {
					var roles = [];
					if (response.status === "ok") {
						for (var key in response.result) {
							var a = response.result[key];
							roles.push(a);
						}
						$.get("groups/roles" + o.url.substring(o.url.indexOf('?')))
						.done(function(resp) {
							var groups = [];
							if (resp.status === "ok") {
								for (var key in resp.result) {
									var a = resp.result[key],
									r = [];
									if (a.roles[0] == null) {
										a.roles = [];
									}
									for (var idx in roles) {
										if ($.inArray(roles[idx].id, a.roles) > -1) {
											r.push({roleid : roles[idx].id, rolename : roles[idx].name, checked : "checked "});
										} else {
											r.push({roleid : roles[idx].id, rolename : roles[idx].name, checked : ""});
										}
									}
									a.roles = r;
									groups.push(a);
								}
							}
							$('#list').html(app.template.render("authorizeGroups",
									{ action : o.url, groups : groups}));
						})
						.error(function(data) {app.notify.error(data)});
					}
				})
				.error(function(data) {app.notify.error(data)});
			},

			authorizeGroupsSubmit : function(o) {
				var form = $(o.target).parents("form");
				$.post(form.attr("action"), form.serialize())
				.done(function(r) {
				    var response = JSON.parse(r);
					if (response.status === "ok") {
						app.notify.done(app.i18n.bundle["app.registry.groups.authorized"]);
					} else {
						app.notify.error(response.message);
					}
				})
				.error(function(data) {app.notify.error(data)});
			},

			application : function(o) {
				$.get(o.url)
				.done(function(response) {
					if (response.status === "ok") {
						$('#list').html(app.template.render("application",
								response.result["0"]));
					} else {
						app.notify.error(response.message);
					}
				})
				.error(function(data) {app.notify.error(data)});
			},

			applicationConf : function(o) {
				var form = $(o.target).parents("form");
				$.post(form.attr("action"), form.serialize())
				.done(function(response) {
					if (response.status === "ok") {
						app.notify.done(app.i18n.bundle["app.registry.application.conf.updated"]);
					} else {
						app.notify.error(response.message);
					}
				})
				.error(function(data) {app.notify.error(data)});
			},

			createExternalApp : function(o) {
				$('#list').html(app.template.render("createExternalApp", { action : o.url}));
			},

			createExternalAppSubmit : function(o) {
				var form = $(o.target).parents("form");
				$.post(form.attr("action"), form.serialize())
				.done(function(response) {
					if (response.status === "ok") {
						$('#form-window').empty();
						appRegistry.action.applications({url : "applications/actions?actionType=WORKFLOW"});
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
	appRegistry.init();
});
