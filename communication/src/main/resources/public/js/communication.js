var communication = function(){
	var app = Object.create(protoApp);
	app.scope = "#main";
	app.define ({
		template : {

			confProfils : '<form method="post" action="{{action}}">\
								<table class="striped alternate" summary="">\
									<thead>\
										<tr>\
											<th scope="col"></th>\
											{{#profils}}\
												<th scope="col">{{name}}</th>\
											{{/profils}}\
										</tr>\
									</thead>\
									<tbody>\
									{{#groups}}\
									<tr>\
										<td>{{name}}</td>\
										{{#choices}}\
										<td><input type="checkbox" name="groupProfil" value="{{.}}" /></td>\
										{{/choices}}\
									</tr>\
									{{/groups}}\
									</tbody>\
								</table>\
								<input call="confProfilsSubmit" type="button" value="{{#i18n}}communication.valid{{/i18n}}" />\
							</form>',

			confParentsEnfants : '<form method="post" action="{{action}}">\
								{{#groups}}\
								<label>{{name}}</label>\
								<input type="checkbox" name="groupId" value="{{id}}" /><br />\
								{{/groups}}\
								<input call="confProfilsSubmit" type="button" value="{{#i18n}}communication.valid{{/i18n}}" />\
							</form>',

		},
		action : {

			confProfils: function(o) {
					$.get(o.url)
					.done(function(groups) {
						var g = [];
						for(var k1 in groups) {
							var row = { name : groups[k1].name, choices : [] };
							for (var k2 in groups) {
								row.choices.push(groups[k1].id + "_" + groups[k2].id);
							}
							g.push(row);
						}
						console.log(g);
						$('#list').html(app.template.render("confProfils",
								{ action : o.url, profils : groups, groups : g }));
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
			},

			confParentsEnfants : function(o) {
				$.get("groups/classes/enfants" + o.url.substring(o.url.indexOf('?')))
				.done(function(groups) {
					$('#list').html(app.template.render("confParentsEnfants",
							{ action : o.url, groups : groups }));
				})
				.error(function(data) {app.notify.error(data)});
			},

		}
	});
	return app;
}();

$(document).ready(function(){
	communication.init();
});
