//Copyright. Tous droits réservés. WebServices pour l’Education.
var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };
	var personDataAdaptor = function (d) {
		for (obj in d.result){
			d.result[obj]['notActivated'] = !d.result[obj].code ? false : true;
			d.result[obj]['isProfessor'] = d.result[obj].type === 'Teacher' ? true : false;
			d.result[obj]['isRelative'] = d.result[obj].type === 'Relative' ? true : false;
		}
		return {list : _.values(d.result)};
	};

	var app = Object.create(protoApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			ecole : "\
				{{#list}}<h3>{{name}}</h3>\
				<a call='classes' href='api/classes?id={{id}}'>\
				{{#i18n}}directory.admin.see-classes{{/i18n}}</a> - \
				<a call='users' href='users?profile=Personnel&structureId={{id}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a> - \
				<a href=\"{{id}}\" call=\"addStructureUser\">\
				{{#i18n}}directory.admin.create-user{{/i18n}}</a> - \
				<a href='api/export?id={{id}}'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a>\
				<div id='classes-{{id}}'></div>\
				<div id='people-{{id}}'></div>{{/list}}"
			,
			classes: "\
				{{#list}}<h4><a>{{name}}</a></h4>\
				<a call='personnes' href='api/personnes?id={{classId}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>\
				 - <a href=\"{{classId}}\" call=\"addUser\">\
				{{#i18n}}directory.admin.create-user{{/i18n}}</a>\
				 - <a href='api/export?id={{classId}}'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a><br />\
				<div id='people-{{classId}}'></div>{{/list}}"
			,
			personnes: "\
				<br /><span>\
					{{#list}}\
					{{#userId}}\
					<a call='personne' href='api/details?id={{userId}}'\
						style='\
							{{#notActivated}}color:red;{{/notActivated}}\
							{{#isProfessor}}font-weight:bold;{{/isProfessor}}\
							{{#isRelative}}font-style:italic;{{/isRelative}}\
						'\
					>\
					{{/userId}}\
					{{^userId}}\
					<a call='personne' href='api/details?id={{id}}'>\
					{{/userId}}\
					{{lastName}} {{firstName}}</a> - \
					{{/list}}\
				</span><div id='details'></div>"
			,
			personne : '\
				{{#list}}{{#i18n}}directory.admin.login{{/i18n}} : {{login}} / {{code}} - \
				{{#i18n}}directory.admin.address{{/i18n}} : {{address}} - \
				<a call="sendResetPassword" href="{{login}}">{{#i18n}}directory.admin.reset.password{{/i18n}}</a> {{/list}}'
			,
			personnesEcole :'\
				{{#list}}<input type="checkbox" name="{{userId}}" value="{{userId}}"/>\
				{{lastName}} {{firstName}} - {{/list}}',
			addUser : '<span>{{#i18n}}directory.admin.create-user{{/i18n}}</span>\
				<form action="api/user">\
				{{#classId}}\
				<input type="hidden" name="classId" value="{{classId}}" />\
				{{/classId}}\
				{{^classId}}\
				<input type="hidden" name="structureId" value="{{structureId}}" />\
				{{/classId}}\
				<label>{{#i18n}}directory.admin.lastname{{/i18n}}</label>\
				<input type="text" name="lastname" />\
				<label>{{#i18n}}directory.admin.firstname{{/i18n}}</label>\
				<input type="text" name="firstname" />\
				{{#classId}}\
				<label>{{#i18n}}directory.admin.birthDate{{/i18n}}</label>\
				<input type="text" name="birthDate" />\
				<label>{{#i18n}}directory.admin.type{{/i18n}}</label>\
				<select name="type">\
					<option value="Teacher">{{#i18n}}directory.admin.teacher{{/i18n}}</option>\
					<option value="Student">{{#i18n}}directory.admin.student{{/i18n}}</option>\
					<option value="Relative">{{#i18n}}directory.admin.parent{{/i18n}}</option>\
					<option value="Personnel">{{#i18n}}directory.admin.personnel{{/i18n}}</option>\
				</select>\
				<select name="childrenIds" multiple>\
				{{#childrens}}\
					<option value="{{userId}}">{{firstName}} {{lastName}}</option>\
				{{/childrens}}\
				</select>\
				{{/classId}}\
				{{^classId}}\
				<input type="hidden" name="type" value="Personnel" />\
				{{/classId}}\
				<input call="addUserSubmit" type="button" value="{{#i18n}}directory.admin.create{{/i18n}}" />\
			</form>',
			sendResetPassword : '\
			    <form action="/auth/sendResetPassword">\
			        <label>{{login}}</label>\
                    <input type="hidden" name="login" value="{{login}}" />\
                    <label>{{#i18n}}directory.admin.email{{/i18n}}</label>\
                    <input type="text" name="email" />\
                    <input call="sendResetPasswordSubmit" type="button" value="{{#i18n}}directory.admin.send{{/i18n}}" />\
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
			users : function(o) {
				var id = o.url.substring(o.url.lastIndexOf("=") + 1);
				$.get(o.url)
				.done(function(data){
					$("#people-" + id).html(app.template.render('personnes', {list : data}));
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
			addUser : function(o) {
				$.get("api/personnes?id=" + o.url + "&type=Student")
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
			addStructureUser : function(o) {
				$("#people-" + o.url).html(app.template.render('addUser', { structureId : o.url }));
			},
			addUserSubmit : function(o) {
				var form = $(o.target).parents("form");
				$.post(form.attr("action"), form.serialize())
				.done(function(response) {
					if (response.hasOwnProperty("id")) {
						$('#people-' + form.children("input[name='classId']").attr("value")).empty();
						app.notify.done(app.i18n.bundle["directory.admin.user.created"]);
					} else {
						app.notify.error(response.message);
					}
				})
				.error(function(data) {app.notify.error(data)});
			},
			sendResetPassword : function(o) {
			    $('#details').html(app.template.render("sendResetPassword", { login : o.url }));
			},
            sendResetPasswordSubmit : function(o) {
                var form = $(o.target).parents("form");
                $.post(form.attr("action"), form.serialize())
                .done(function(response) {
                    app.notify.done(app.i18n.bundle["directory.admin.reset.code.sent"]);
                })
                .error(function(data) {
                    app.notify.error(app.i18n.bundle["directory.admin.reset.code.send.error"]);
                });
            }
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init();
});
