var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };
	var personDataAdaptor = function (d) {
		for (obj in d.result){
			d.result[obj]['notActivated'] = d.result[obj].code === '' ? false : true;
			d.result[obj]['isProfessor'] = d.result[obj].type === 'ENSEIGNANT' ? true : false;
		}
		return {list : _.values(d.result)};
	};

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			ecole : "\
				{{#list}}<h3>{{name}}</h3>\
				<a call='classes' href='api/classes?id={{id}}'>\
				{{#i18n}}directory.admin.see-classes{{/i18n}}</a> - \
				<a href='api/export?id={{id}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a>\
				<div id='classes-{{id}}'></div>{{/list}}"
			,
			classes: "\
				{{#list}}<h4><a>{{name}}</a></h4>\
				<a call='personnes' href='api/personnes?id={{classId}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>\
				 - <a href=\"{{classId}}\" call=\"addUser\">\
				{{#i18n}}directory.admin.create-user{{/i18n}}</a>\
				 - <a href='api/export?id={{classId}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a><br />\
				<div id='people-{{classId}}'></div>{{/list}}"
			,
			personnes: "\
				<br /><span>\
					{{#list}}\
					<a call='personne' href='api/details?id={{userId}}'\
						style='\
							{{#notActivated}}color:red;{{/notActivated}}\
							{{#isProfessor}}font-weight:bold;{{/isProfessor}}\
						'\
					>\
					{{lastName}} {{firstName}}</a> - \
					{{/list}}\
				</span><div id='details'></div>"
			,
			personne : '\
				{{#list}}{{#i18n}}directory.admin.login{{/i18n}} : {{login}} / {{code}} - \
				{{#i18n}}directory.admin.address{{/i18n}} : {{address}}{{/list}}'
			,
			exportAuth : 'Nom,Prénom,Login,Mot de passe\n'
				+ '{{#list}}{{lastName}},{{firstName}},{{login}},{{activationCode}}\n'
				+ '{{/list}}'
			,
			personnesEcole :'\
				{{#list}}<input type="checkbox" name="{{userId}}" value="{{userId}}"/>\
				{{lastName}} {{firstName}} - {{/list}}',
			addUser : '<span>{{#i18n}}directory.admin.create-user{{/i18n}}</span>\
				<form action="api/user">\
				<input type="hidden" name="classId" value="{{classId}}" />\
				<label>{{#i18n}}directory.admin.lastname{{/i18n}}</label>\
				<input type="text" name="lastname" />\
				<label>{{#i18n}}directory.admin.firstname{{/i18n}}</label>\
				<input type="text" name="firstname" />\
				<label>{{#i18n}}directory.admin.type{{/i18n}}</label>\
				<select name="type">\
					<option value="ENSEIGNANT">{{#i18n}}directory.admin.teacher{{/i18n}}</option>\
					<option value="ELEVE">{{#i18n}}directory.admin.student{{/i18n}}</option>\
					<option value="PERSRELELEVE">{{#i18n}}directory.admin.parent{{/i18n}}</option>\
				</select>\
				<select name="childrenIds" multiple>\
				{{#childrens}}\
					<option value="{{userId}}">{{firstName}} {{lastName}}</option>\
				{{/childrens}}\
				</select>\
				<input call="addUserSubmit" type="button" value="{{#i18n}}directory.admin.create{{/i18n}}" />\
			</form>'
		},
		action : {
			ecole : function(o) {
				if (!!$('#schools').children().length) {
					$('#schools').html('');
					return;
				}
				app.template.getAndRender(o.url, 'ecole','#schools', dataExtractor);
			},
			classes : function(o) {
				$.get(o.url)
				.done(function(data){
					if(undefined !== data.result[0]){
						if (!!$("#classes-" + data.result[0]["schoolId"]).children().length) {
							$("#classes-" + data.result[0]["schoolId"]).html('');
							return;
						}
						$("#classes-" + data.result[0]["schoolId"]).html(app.template.render('classes', dataExtractor(data)));
					} else {
						app.notify.info("Aucun résultat");
					}
				})
				.error(function(data){app.notify.error(data);})
			},
			personnesEcole : function(o) {
				app.template.getAndRender(o.url, 'personnesEcole', '#users', dataExtractor);
			},
			personnes : function(o) {
				$.get(o.url)
				.done(function(data){
					if (!!$('#people-' + data.result[0]["classId"]).children().length) {
						$('#people-' + data.result[0]["classId"]).html('');
						return;
					}
					$("#people-" + data.result[0]["classId"]).html(app.template.render('personnes', personDataAdaptor(data)));
				})
				.error(function(data){app.notify.error(data)})
			},

			personne : function(o) {
				if (!!$('#details').children('form').length) {
					$('#details').html('');
					return;
				}
				app.template.getAndRender(o.url, 'personne', '#details', dataExtractor);
			},
			exportAuth : function(o) {
				$.get(o.url)
				.done(function(data){
					document.location = 'data:Application/octet-stream,'
					+ encodeURIComponent(app.template.render('exportAuth', dataExtractor(data)));
					app.notify.info("Ok");
				})
				.error(function(data){app.notify.error(data)})
			},
			addUser : function(o) {
				$.get("api/personnes?id=" + o.url + "&type=ELEVE")
				.done(function(response) {
					var childrens = [];
					if (response.status === "ok") {
						for (var key in response.result) {
							childrens.push(response.result[key]);
						}
						$("#people-" + o.url).html(app.template.render('addUser',
								{ classId : o.url, childrens : childrens }));
					}
				});
			},
			addUserSubmit : function(o) {
				var form = $(o.target).parents("form");
				$.post(form.attr("action"), form.serialize())
				.done(function(response) {
					if (response.status === "ok") {
						$('#people-' + form.children("input[name='classId']").attr("value")).empty();
						app.notify.done(app.i18n.bundle["directory.admin.user.created"]);
					} else {
						app.notify.error(response.message);
					}
				})
				.error(function(data) {app.notify.error(data)});
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init();
});
