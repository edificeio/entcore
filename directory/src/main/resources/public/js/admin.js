var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(jQuery.parseJSON(d).result)}; };
//	normaliser = function (d) { return d.replace(/ /g,'_').replace(/\$/g, '#').replace(/\(/g,'/').replace(/\)/g,'\\'); };

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			ecole : "\
				{{#list}}<h3>{{name}}</h3>\
				<a call='classes' href='/api/classes?id={{id}}'>\
				{{#i18n}}directory.admin.see-classes{{/i18n}}</a> - \
				<a href='/api/export?id={{id}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a>\
				<div id='classes-{{id}}'></div>{{/list}}"
			,
			groupes : "\
				<h3>{{nENTGroupeNom}}</h3>\
				<a call='membres' href='/api/membres?data={{nENTPeople}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>"
			,
			classes: "\
				{{#list}}<h4><a>{{name}}</a></h4>\
				<a call='personnes' href='/api/personnes?id={{classId}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>\
				 - <a href='/api/enseignants?id={{classId}}' call='enseignants'>\
				{{#i18n}}directory.admin.add-teacher{{/i18n}}</a>\
				 - <a href='/api/export?id={{classId}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a><br />\
				<div id='people-{{classId}}'></div>{{/list}}"
			,
			personnes: "\
				<br /><span>{{#list}}<a call='personne' href='/api/details?id={{userId}}'>\
				{{lastName}} {{firstName}}</a> - <div id='details'></div>{{/list}}</span>"
			,
			enseignants : "\
				<br /><span>{{#list}}\
				<a call='personne' href='/api/link?class={{classId}}&id={{userId}}'>\
				{{lastName}} {{firstName}}</a> - {{/list}}</span>"
			,
			membres : "\
				<span>{{#list}}{{lastName}} {{firstName}} - {{/list}}</span>"
			,
			personne : '\
				{{#i18n}}directory.admin.lastname{{/i18n}}{{lastName}} - \
				{{#i18n}}directory.admin.firstname{{/i18n}}{{firstName}} - \
				{{#i18n}}directory.admin.address{{/i18n}}{{address}}'
			,
			exportAuth : '\
				Nom,Prénom,Login,Mot de passe\n{{#list}}\
				{{lastName}},{{firstName}},{{login}},{{password}}\n{{/list}}'
			,
			personnesEcole :'\
				{{#list}}<input type="checkbox" name="{{userId}}" value="{{userId}}"/>\
				{{lastName}} {{firstName}} - {{/list}}'
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
					var jo = jQuery.parseJSON(data);
					if (!!$("#classes-" + jo.result[0]["schoolId"]).children().length) {
						$("#classes-" + jo.result[0]["schoolId"]).html('');
						return;
					}
					$("#classes-" + jo.result[0]["schoolId"]).html(app.template.render('classes', dataExtractor(jo)));
				})
				.error(function(data){app.notify.error(data);})
			},
			groupes : function(o) {
				if (!!$('#groups').children().length) {
					$('#groups').html('');
					return;
				}
				app.template.getAndRender(o.url, 'groupes', '#groups', dataExtractor);
			},
			personnesEcole : function(o) {
				app.template.getAndRender(o.url, 'personnesEcole', '#users', dataExtractor);
			},
			personnes : function(o) {
				$.get(o.url)
				.done(function(data){
					var jo = jQuery.parseJSON(data);
					if (!!$('#people-' + jo.result[0]["classId"]).children().length) {
						$('#people-' + jo.result[0]["classId"]).html('');
						return;
					}
					$("#people-" + jo.result[0]["schoolId"]).html(app.template.render('personnes', dataExtractor(jo)));
				})
				.error(function(data){app.notify.error(data)})
			},
			membres : function(o) {
				if (!!$('#members').children('form').length) {
					$('#members').html('');
					return;
				}
				app.template.getAndRender(o.url, 'membres', '#members', dataExtractor);
			},
			personne : function(o) {
				if (!!$('#details').children('form').length) {
					$('#details').html('');
					return;
				}
				app.template.getAndRender(o.url, 'personne', '#details', dataExtractor);
			},
			enseignants : function(o) {
				$.get(o.url)
				.done(function(data){
					var jo = jQuery.parseJSON(data);
					if (!!$('#people-' + jo.result[0]["classId"]).children().length) {
						$('#people-' + jo.result[0]["classId"]).html('');
						return;
					}
					$("#people-" + jo.result[0]["schoolId"]).html(app.template.render('enseignants', dataExtractor(jo)));
				})
				.error(function(data){app.notify.error(data)})
			},
			exportAuth : function(o) {
				document.location = 'data:Application/octet-stream,'
					+ encodeURIComponent(app.template.getAndRender(o.url, 'exportAuth'));
			},
			createUser : function(o) {
				var url = o.target.form.action + '?' + $('#create-user').serialize()
					+ '&ENTPersonProfils=' + $('#profile').val()
					+ '&ENTPersonStructRattach=' + $('#groupe').val().replace(/ /g,'-');
				$.get(url)
				.done(function(data){
					if (data.result === "error"){
						$('label').removeAttr('style');
						for (obj in data){
							if (obj !== "result"){
								$('#' + obj).attr("style", "color:red");
							}
						}
						app.notify.info("{{#i18n}}directory.admin.ok{{/i18n}}");
					} else {
						app.notify.done("{{#i18n}}directory.admin.form-error{{/i18n}}");
						$('label').removeAttr('style');
					}
				})
				.error(function(data){app.notify.error(jQuery.parseJSON(data).status);})
			},
			createAdmin : function(o) {
				var url = o.target.form.action + '?' + $('#create-admin').serialize()
					+ '&ENTPerson=' + $('#choice').val();
				$.get(url)
				.done(function(data){app.notify.done(jQuery.parseJSON(data).status);})
				.error(function(data){app.notify.error(jQuery.parseJSON(data).status);})
			},
			createGroup : function(o) {
				var url = o.target.form.action + '?' + $('#create-group').serialize()
					+ '&type=' + $('#type').val() + '&ENTGroupStructRattach=' + $('#parent').val();
				$.get(url)
				.done(function(data){app.notify.done(jQuery.parseJSON(data).status);})
				.error(function(data){app.notify.error(jQuery.parseJSON(data).status);})
			},
			createSchool : function(o) {
				var url = o.target.form.action + '?' + $('#create-school').serialize();
				$.get(url)
				.done(function(data){app.notify.done(jQuery.parseJSON(data).status);})
				.error(function(data){app.notify.error(jQuery.parseJSON(data).status);})
			},
			view: function(o) {
				switch(o.target.id){
					case 'disp':
						$('#creation').attr('hidden', '');
						$('#display').removeAttr('hidden');
						$('#export').attr('hidden', '');
						break;
					case 'exports':
						$('#creation').attr('hidden', '');
						$('#display').attr('hidden');
						$('#export').removeAttr('hidden');
						break;
					case 'create':
						$('#creation').removeAttr('hidden');
						$('#display').attr('hidden', '');
						$('#export').attr('hidden', '');
						break;
				}
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init(); 
});